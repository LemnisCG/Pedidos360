package cl.duoc.pedidos360.envio.listener;

import cl.duoc.pedidos360.envio.event.PagoCompletadoEvent;
import cl.duoc.pedidos360.envio.model.Envio;
import cl.duoc.pedidos360.envio.repository.EnvioRepository;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Consumidor de eventos de pago completado.
 * Usa confirmación manual (ACK/NACK) para garantizar que ningún mensaje se pierda.
 * Los mensajes que fallan son enviados a la DLQ para inspección posterior.
 */
@Component
public class PagoCompletadoListener {

    private static final Logger log = LoggerFactory.getLogger(PagoCompletadoListener.class);

    private final EnvioRepository envioRepository;
    private final JavaMailSender mailSender;
    private final boolean mailEnabled;
    private final String mailFrom;
    private final String frontendPublicUrl;

    public PagoCompletadoListener(
            EnvioRepository envioRepository,
            JavaMailSender mailSender,
            @Value("${mail.enabled}") boolean mailEnabled,
            @Value("${mail.from}") String mailFrom,
            @Value("${frontend.public-url}") String frontendPublicUrl
    ) {
        this.envioRepository = envioRepository;
        this.mailSender = mailSender;
        this.mailEnabled = mailEnabled;
        this.mailFrom = mailFrom;
        this.frontendPublicUrl = frontendPublicUrl;
    }

    @RabbitListener(queues = "${rabbitmq.queue.envio}")
    public void onPagoCompletado(
            PagoCompletadoEvent event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) throws IOException {
        log.info("Mensaje recibido para pedido: {}", event.numeroPedido());

        try {
            // 1. Persistir el registro de envío
            Envio envio = new Envio();
            envio.setPedidoId(event.pedidoId());
            envio.setNumeroPedido(event.numeroPedido());
            envio.setUsuarioId(event.usuarioId());
            envio.setEmail(event.email());
            envio.setDireccion(event.direccion());
            envio.setEstado("PREPARANDO");

            // 2. Intentar enviar correo si SMTP está habilitado
            boolean emailEnviado = false;
            if (mailEnabled) {
                try {
                    sendEmail(event);
                    emailEnviado = true;
                    log.info("Email enviado exitosamente para pedido: {}", event.numeroPedido());
                } catch (Exception e) {
                    log.error("Error al enviar email para pedido {}: {}", event.numeroPedido(), e.getMessage());
                    // El email falló pero el envío se registra igual
                }
            }

            envio.setEmailEnviado(emailEnviado);
            envioRepository.save(envio);

            // 3. ACK: mensaje procesado correctamente, se elimina de la cola
            channel.basicAck(deliveryTag, false);
            log.info("ACK enviado para pedido: {}", event.numeroPedido());

        } catch (Exception e) {
            // 4. NACK: error irrecuperable, enviar a la DLQ (requeue=false)
            log.error("Error procesando pedido {}. Enviando a DLQ: {}", event.numeroPedido(), e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private void sendEmail(PagoCompletadoEvent event) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(mailFrom);
        helper.setTo(event.email());
        helper.setSubject("Pedidos360 · Compra " + event.numeroPedido() + " confirmada");
        helper.setText(buildHtml(event), true);
        mailSender.send(message);
    }

    private String buildHtml(PagoCompletadoEvent event) {
        NumberFormat clp = NumberFormat.getCurrencyInstance(new Locale("es", "CL"));
        StringBuilder rows = new StringBuilder();

        for (PagoCompletadoEvent.ItemEvent item : event.items()) {
            String imageUrl = resolveImageUrl(item.imagenUrl());
            rows.append("<tr>")
                    .append("<td style='padding:8px'>")
                    .append("<img src='").append(escape(imageUrl))
                    .append("' width='48' height='64' style='object-fit:contain;border-radius:6px'>")
                    .append("</td>")
                    .append("<td><b>").append(escape(item.nombre())).append("</b><br>")
                    .append("Cantidad: ").append(item.cantidad()).append("</td>")
                    .append("<td>").append(clp.format(item.precio() * item.cantidad())).append("</td>")
                    .append("</tr>");
        }

        return "<div style='font-family:Arial,sans-serif;max-width:620px;margin:auto'>"
                + "<h2 style='color:#16a34a'>✓ Pago confirmado</h2>"
                + "<p>Hola " + escape(event.nombre()) + ", tu pedido <b>"
                + escape(event.numeroPedido()) + "</b> fue registrado correctamente.</p>"
                + "<table style='width:100%;border-collapse:collapse'>" + rows + "</table>"
                + "<hr><p><b>Total:</b> " + clp.format(event.total()) + "</p>"
                + "<p><b>Dirección de envío:</b> " + escape(event.direccion()) + "</p>"
                + "<p style='color:#6b7280'>Gracias por comprar en Pedidos360.</p></div>";
    }

    private String resolveImageUrl(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return "";
        }
        return imagePath.startsWith("http") ? imagePath : frontendPublicUrl + imagePath;
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}

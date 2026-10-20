package cl.duoc.pedidos360.envio.service;

import cl.duoc.pedidos360.envio.event.PagoCompletadoEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.util.Locale;

/** Construye y envía por SMTP el comprobante HTML de una compra. No conoce nada de RabbitMQ. */
@Service
public class CorreoConfirmacionCompraService {

    private final JavaMailSender mailSender;
    private final String mailFrom;
    private final String frontendPublicUrl;

    public CorreoConfirmacionCompraService(
            JavaMailSender mailSender,
            @Value("${mail.from}") String mailFrom,
            @Value("${frontend.public-url}") String frontendPublicUrl
    ) {
        this.mailSender = mailSender;
        this.mailFrom = mailFrom;
        this.frontendPublicUrl = frontendPublicUrl;
    }

    /** Lanza MailSendException si el servidor SMTP no responde y MailAuthenticationException si rechaza las credenciales. */
    public void enviarConfirmacionDeCompra(PagoCompletadoEvent event) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(event.email());
            helper.setSubject("Pedidos360 · Compra " + event.numeroPedido() + " confirmada");
            helper.setText(buildHtml(event), true);
        } catch (MessagingException e) {
            throw new MailPreparationException("No se pudo construir el correo del pedido " + event.numeroPedido(), e);
        }
        mailSender.send(message);
    }

    private String buildHtml(PagoCompletadoEvent event) {
        NumberFormat clp = NumberFormat.getCurrencyInstance(new Locale("es", "CL"));
        StringBuilder rows = new StringBuilder();

        if (event.items() != null) {
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

    /** Escapado mínimo para que datos del usuario no se interpreten como HTML en el correo. */
    private String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}

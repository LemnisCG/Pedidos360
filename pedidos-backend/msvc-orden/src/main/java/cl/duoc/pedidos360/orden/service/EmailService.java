package cl.duoc.pedidos360.orden.service;

import cl.duoc.pedidos360.orden.model.Orden;
import cl.duoc.pedidos360.orden.model.OrdenItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.NumberFormat;
import java.util.Locale;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String from;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${pedidos360.mail.enabled:false}") boolean enabled,
            @Value("${pedidos360.mail.from:no-reply@pedidos360.local}") String from
    ) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.from = from;
    }

    /**
     * Envía una confirmación REAL por SMTP cuando MAIL_ENABLED=true y existen
     * credenciales válidas. Si el correo no está configurado, la orden igual
     * queda guardada y el frontend puede informar que el envío está pendiente.
     */
    public boolean enviarConfirmacionPago(Orden orden) {
        if (!enabled) {
            log.info("Correo deshabilitado. Configura MAIL_ENABLED=true para enviar la confirmación de la orden {}.", orden.getNumeroOrden());
            return false;
        }

        if (!StringUtils.hasText(orden.getClienteEmail())) {
            log.warn("La orden {} no tiene correo de cliente; no se puede enviar confirmación.", orden.getNumeroOrden());
            return false;
        }

        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(from);
            mensaje.setTo(orden.getClienteEmail());
            mensaje.setSubject("Pedidos360 | Pago confirmado - Pedido " + orden.getNumeroOrden());
            mensaje.setText(construirCuerpo(orden));
            mailSender.send(mensaje);
            log.info("Confirmación de pago enviada a {} para la orden {}.", orden.getClienteEmail(), orden.getNumeroOrden());
            return true;
        } catch (Exception ex) {
            log.error("No fue posible enviar el correo de la orden {}: {}", orden.getNumeroOrden(), ex.getMessage());
            return false;
        }
    }

    private String construirCuerpo(Orden orden) {
        NumberFormat clp = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CL"));
        StringBuilder detalle = new StringBuilder();

        for (OrdenItem item : orden.getItems()) {
            detalle.append("- ")
                    .append(item.getNombre())
                    .append(" x ")
                    .append(item.getCantidad())
                    .append(" | ")
                    .append(clp.format(item.getPrecioUnitario()))
                    .append(System.lineSeparator());
        }

        return "Hola," + System.lineSeparator() + System.lineSeparator()
                + "Tu pago en Pedidos360 fue aprobado correctamente." + System.lineSeparator()
                + "Número de pedido: " + orden.getNumeroOrden() + System.lineSeparator()
                + "Estado: " + orden.getEstado() + System.lineSeparator()
                + "Total: " + clp.format(orden.getTotal()) + System.lineSeparator()
                + "Dirección de envío: " + orden.getDireccionEnvio() + System.lineSeparator()
                + System.lineSeparator()
                + "Productos:" + System.lineSeparator()
                + detalle
                + System.lineSeparator()
                + "Importante: el módulo de pago de este proyecto es una simulación académica; este correo confirma el flujo registrado por Pedidos360." + System.lineSeparator()
                + System.lineSeparator()
                + "Pedidos360";
    }
}

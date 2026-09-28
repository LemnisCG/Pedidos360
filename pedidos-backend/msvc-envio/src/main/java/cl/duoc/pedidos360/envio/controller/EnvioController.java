package cl.duoc.pedidos360.envio.controller;

import cl.duoc.pedidos360.envio.model.Envio;
import cl.duoc.pedidos360.envio.repository.EnvioRepository;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Persiste la solicitud de envío y, cuando SMTP está habilitado,
 * envía un comprobante HTML al correo asociado a la identidad autenticada.
 */
@RestController
public class EnvioController {

    private final EnvioRepository repository;
    private final JavaMailSender mailSender;
    private final boolean mailEnabled;
    private final String mailFrom;
    private final String frontendPublicUrl;

    public EnvioController(
            EnvioRepository repository,
            JavaMailSender mailSender,
            @Value("${mail.enabled}") boolean mailEnabled,
            @Value("${mail.from}") String mailFrom,
            @Value("${frontend.public-url}") String frontendPublicUrl
    ) {
        this.repository = repository;
        this.mailSender = mailSender;
        this.mailEnabled = mailEnabled;
        this.mailFrom = mailFrom;
        this.frontendPublicUrl = frontendPublicUrl;
    }

    /** Producto resumido que aparecerá en el correo de confirmación. */
    public record Item(String nombre, int cantidad, long precio, String imagenUrl) {
    }

    /** Mensaje interno enviado por msvc-pago después de confirmar el pedido. */
    public record Confirmacion(
            UUID pedidoId,
            String numeroPedido,
            UUID usuarioId,
            String nombre,
            String email,
            String direccion,
            long total,
            List<Item> items
    ) {
    }

    /** Permite que msvc-pago muestre si el correo realmente salió por SMTP. */
    public record Result(boolean emailEnviado, UUID envioId) {
    }

    @PostMapping("/internal/envios/confirmacion")
    public Result confirmar(@RequestBody Confirmacion request) {
        Envio envio = new Envio();
        envio.setPedidoId(request.pedidoId());
        envio.setNumeroPedido(request.numeroPedido());
        envio.setUsuarioId(request.usuarioId());
        envio.setEmail(request.email());
        envio.setDireccion(request.direccion());
        envio.setEstado("PREPARANDO");

        boolean enviado = false;
        if (mailEnabled) {
            try {
                sendEmail(request);
                enviado = true;
            } catch (Exception ignored) {
                // La compra permanece válida aunque el proveedor SMTP esté temporalmente caído.
                enviado = false;
            }
        }

        envio.setEmailEnviado(enviado);
        envio = repository.save(envio);
        return new Result(enviado, envio.getId());
    }

    /** Construye y envía el mensaje MIME como HTML UTF-8. */
    private void sendEmail(Confirmacion request) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(mailFrom);
        helper.setTo(request.email());
        helper.setSubject("Pedidos360 · Compra " + request.numeroPedido() + " confirmada");
        helper.setText(buildHtml(request), true);
        mailSender.send(message);
    }

    /** Crea un comprobante sencillo con portada, cantidad, total y dirección de envío. */
    private String buildHtml(Confirmacion request) {
        NumberFormat clp = NumberFormat.getCurrencyInstance(new Locale("es", "CL"));
        StringBuilder rows = new StringBuilder();

        for (Item item : request.items()) {
            String imageUrl = imageUrl(item.imagenUrl());
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
                + "<p>Hola " + escape(request.nombre()) + ", tu pedido <b>"
                + escape(request.numeroPedido()) + "</b> fue registrado correctamente.</p>"
                + "<table style='width:100%;border-collapse:collapse'>" + rows + "</table>"
                + "<hr><p><b>Total:</b> " + clp.format(request.total()) + "</p>"
                + "<p><b>Dirección de envío:</b> " + escape(request.direccion()) + "</p>"
                + "<p style='color:#6b7280'>Gracias por comprar en Pedidos360.</p></div>";
    }

    private String imageUrl(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return "";
        }
        return imagePath.startsWith("http") ? imagePath : frontendPublicUrl + imagePath;
    }

    /** Escapado mínimo para que datos del usuario no se interpreten como HTML en el correo. */
    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}

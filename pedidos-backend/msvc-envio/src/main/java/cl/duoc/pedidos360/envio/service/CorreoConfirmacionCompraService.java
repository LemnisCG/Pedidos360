package cl.duoc.pedidos360.envio.service;

import cl.duoc.pedidos360.envio.event.PagoCompletadoEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
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
            List<InlineImage> images = new ArrayList<>();
            helper.setText(buildHtml(event, images), true);
            for (InlineImage image : images) {
                helper.addInline(image.cid(), image.resource(), image.contentType());
            }
        } catch (MessagingException e) {
            throw new MailPreparationException("No se pudo construir el correo del pedido " + event.numeroPedido(), e);
        }
        mailSender.send(message);
    }

    private record InlineImage(String cid, ClassPathResource resource, String contentType) {
    }

    /** Correo HTML responsive compatible con Gmail y clientes de correo comunes. */
    private String buildHtml(PagoCompletadoEvent event, List<InlineImage> images) {
        NumberFormat clp = NumberFormat.getCurrencyInstance(new Locale("es", "CL"));
        StringBuilder rows = new StringBuilder();
        if (event.items() != null) {
            for (PagoCompletadoEvent.ItemEvent item : event.items()) {
                String imageUrl = "";
                String path = item.imagenUrl();
                if (path != null && path.matches("/assets/games/[a-zA-Z0-9._-]+\\.(webp|jpg|jpeg|png)")) {
                    String filename = path.substring(path.lastIndexOf('/') + 1);
                    String recursoFinal = filename.endsWith(".webp") ? filename + ".jpg" : filename;
                    ClassPathResource resource = new ClassPathResource("mail-images/" + recursoFinal);
                    if (resource.exists()) {
                        String cid = "producto-" + images.size();
                        String type = recursoFinal.endsWith(".png") ? "image/png" : "image/jpeg";
                        images.add(new InlineImage(cid, resource, type));
                        imageUrl = "cid:" + cid;
                    }
                }
                rows.append("<tr><td style='padding:13px 7px;border-bottom:1px solid #334155;width:66px'>");
                if (!imageUrl.isBlank()) {
                    rows.append("<img alt='' src='").append(escape(imageUrl))
                            .append("' width='50' height='65' style='object-fit:contain;border-radius:5px;background:white'>");
                }
                rows.append("</td><td style='padding:13px 8px;border-bottom:1px solid #334155;color:#f8fafc'>")
                        .append("<strong>").append(escape(item.nombre())).append("</strong><br>")
                        .append("<span style='color:#cbd5e1;font-size:13px'>Cantidad: ").append(item.cantidad())
                        .append("</span></td><td align='right' style='padding:13px 7px;border-bottom:1px solid #334155;color:#f8fafc;white-space:nowrap'>")
                        .append(escape(clp.format(item.precio() * item.cantidad()))).append("</td></tr>");
            }
        }
        String url = frontendPublicUrl.replaceAll("/+$", "") + "/orders/" + event.pedidoId();
        return """
            <!doctype html><html lang="es"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1"></head>
            <body style="margin:0;padding:0;background:#eef2f7;font-family:Arial,Helvetica,sans-serif">
              <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%%" style="background:#eef2f7;padding:28px 12px"><tr><td align="center">
                <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%%" style="max-width:640px;border-collapse:collapse;background:#111827;border-radius:14px;overflow:hidden">
                  <tr><td align="center" style="padding:30px 24px;background:#17243b">
                    <div style="font-size:27px;color:#fff;font-weight:800">🛒 Pedidos<span style="color:#60a5fa">360</span></div>
                    <p style="color:#9dbde3;font-size:12px;letter-spacing:2px">CONFIRMACIÓN DE COMPRA</p>
                    <h1 style="color:#fff;font-size:27px;margin:15px 0 10px">¡Tu pedido ha sido realizado!</h1>
                    <p style="color:#dbeafe;font-size:15px;line-height:1.6">Hola %s, registramos tu compra correctamente.</p>
                    <span style="display:inline-block;background:#dcfce7;color:#166534;padding:9px 16px;border-radius:30px;font-weight:bold;font-size:13px">Pago confirmado</span>
                  </td></tr>
                  <tr><td style="padding:26px 26px 8px;color:#f8fafc">
                    <table role="presentation" width="100%%" style="border-collapse:collapse;background:#1f2937;border-radius:10px"><tr><td style="padding:18px">
                      <span style="color:#94a3b8;font-size:13px">Número de pedido</span><br><b style="font-size:19px;color:white">%s</b>
                    </td></tr></table>
                    <h2 style="font-size:19px;margin:27px 0 12px;color:#fff;border-bottom:2px solid #2563eb;padding-bottom:10px">Productos de tu compra</h2>
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="border-collapse:collapse">%s</table>
                    <table role="presentation" width="100%%" style="border-collapse:collapse;margin:20px 0;background:#1f2937;border-radius:10px;color:#fff"><tr>
                      <td style="padding:18px;color:#dbeafe">Total</td><td align="right" style="padding:18px;font-size:21px;font-weight:800">%s</td>
                    </tr></table>
                    <h2 style="font-size:17px;color:#fff;margin:24px 0 8px">Información de entrega</h2>
                    <p style="color:#cbd5e1;line-height:1.7;margin:0;overflow-wrap:anywhere">%s</p>
                    <h2 style="font-size:17px;color:#fff;margin:24px 0 8px">Método de pago</h2>
                    <p style="color:#cbd5e1;line-height:1.7;margin:0">Tarjeta · Confirmación realizada correctamente</p>
                    <div style="text-align:center;padding:32px 0 23px"><a href="%s" style="display:inline-block;background:#0d6efd;color:#fff;text-decoration:none;padding:14px 25px;border-radius:8px;font-weight:bold">Ver detalle del pedido</a></div>
                  </td></tr>
                  <tr><td align="center" style="padding:20px;background:#0f172a;color:#a3b4cb;font-size:12px;line-height:1.6">Gracias por comprar en Pedidos360.<br>Tu comprobante fue generado correctamente.</td></tr>
                </table>
              </td></tr></table>
            </body></html>
            """.formatted(
                escape(event.nombre()),
                escape(event.numeroPedido()),
                rows.toString(),
                escape(clp.format(event.total())),
                escape(event.direccion()),
                escape(url)
        );
    }

    /** Escapado mínimo para que datos del usuario no se interpreten como HTML en el correo. */
    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}

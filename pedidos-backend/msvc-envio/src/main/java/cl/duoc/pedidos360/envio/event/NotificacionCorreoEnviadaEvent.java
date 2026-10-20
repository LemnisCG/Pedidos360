package cl.duoc.pedidos360.envio.event;

import java.util.UUID;

/**
 * Evento publicado cuando el correo de confirmación de un pedido salió por SMTP.
 * msvc-pago lo consume para marcar el pedido con emailEnviado = true.
 */
public record NotificacionCorreoEnviadaEvent(UUID pedidoId, String numeroPedido) {
}

package cl.duoc.pedidos360.pago.event;

import java.util.UUID;

/**
 * Réplica del evento que publica msvc-envio cuando el correo de confirmación salió por SMTP.
 * Debe coincidir campo a campo para que Jackson lo deserialice correctamente.
 */
public record NotificacionCorreoEnviadaEvent(UUID pedidoId, String numeroPedido) {
}

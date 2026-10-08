package cl.duoc.pedidos360.envio.event;

import java.util.List;
import java.util.UUID;

/**
 * Réplica exacta del evento publicado por msvc-pago.
 * Debe coincidir campo a campo para que Jackson lo deserialice correctamente.
 */
public record PagoCompletadoEvent(
        UUID pedidoId,
        String numeroPedido,
        UUID usuarioId,
        String nombre,
        String email,
        String direccion,
        long total,
        List<ItemEvent> items
) {
    public record ItemEvent(String nombre, int cantidad, long precio, String imagenUrl) {
    }
}

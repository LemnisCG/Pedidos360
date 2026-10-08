package cl.duoc.pedidos360.pago.event;

import java.util.List;
import java.util.UUID;

/**
 * Evento publicado a RabbitMQ cuando un pago se completa exitosamente.
 * Contiene toda la información que msvc-envio necesita para procesar el envío
 * sin hacer llamadas HTTP de vuelta a msvc-pago.
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

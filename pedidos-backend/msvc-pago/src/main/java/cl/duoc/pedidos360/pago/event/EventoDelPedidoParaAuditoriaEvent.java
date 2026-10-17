package cl.duoc.pedidos360.pago.event;

import java.util.UUID;

/**
 * Evento publicado en el exchange topic pedidos.eventos.topic cada vez que le ocurre algo a un pedido.
 * tipoEvento repite la routing key con que se publicó (por ejemplo pedido.pagado).
 * Debe coincidir campo a campo con el evento que consume msvc-envio.
 */
public record EventoDelPedidoParaAuditoriaEvent(UUID pedidoId, String numeroPedido, String tipoEvento) {
}

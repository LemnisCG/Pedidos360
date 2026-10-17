package cl.duoc.pedidos360.envio.event;

import java.util.UUID;

/**
 * Evento publicado en el exchange topic pedidos.eventos.topic cada vez que le ocurre algo a un pedido.
 * tipoEvento repite la routing key con que se publicó (por ejemplo pedido.envio.registrado): viaja en el cuerpo
 * porque al pasar por la cola de reintento la routing key original se pierde.
 * Debe coincidir campo a campo con el evento que publica msvc-pago.
 */
public record EventoDelPedidoParaAuditoriaEvent(UUID pedidoId, String numeroPedido, String tipoEvento) {
}

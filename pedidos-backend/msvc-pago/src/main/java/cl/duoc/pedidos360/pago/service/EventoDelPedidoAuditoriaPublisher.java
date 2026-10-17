package cl.duoc.pedidos360.pago.service;

import cl.duoc.pedidos360.pago.event.EventoDelPedidoParaAuditoriaEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Publica en el exchange topic pedidos.eventos.topic que un pedido fue pagado.
 * Es solo auditoría: a diferencia de PagoEventPublisher, no espera la confirmación del broker.
 */
@Service
public class EventoDelPedidoAuditoriaPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final String eventosDelPedidoTopicExchangeName;
    private final String pedidoPagadoRoutingKey;

    public EventoDelPedidoAuditoriaPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${rabbitmq.exchange.eventos-del-pedido-topic}") String eventosDelPedidoTopicExchangeName,
            @Value("${rabbitmq.routing-key.pedido-pagado}") String pedidoPagadoRoutingKey
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.eventosDelPedidoTopicExchangeName = eventosDelPedidoTopicExchangeName;
        this.pedidoPagadoRoutingKey = pedidoPagadoRoutingKey;
    }

    public void publicarPedidoPagado(UUID pedidoId, String numeroPedido) {
        rabbitTemplate.convertAndSend(eventosDelPedidoTopicExchangeName, pedidoPagadoRoutingKey,
                new EventoDelPedidoParaAuditoriaEvent(pedidoId, numeroPedido, pedidoPagadoRoutingKey));
    }
}

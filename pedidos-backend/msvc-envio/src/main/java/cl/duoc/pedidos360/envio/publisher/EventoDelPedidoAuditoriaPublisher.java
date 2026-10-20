package cl.duoc.pedidos360.envio.publisher;

import cl.duoc.pedidos360.envio.event.EventoDelPedidoParaAuditoriaEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Publica en el exchange topic pedidos.eventos.topic lo que msvc-envio hizo con cada pedido. */
@Component
public class EventoDelPedidoAuditoriaPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final String eventosDelPedidoTopicExchangeName;
    private final String envioRegistradoRoutingKey;
    private final String correoEnviadoRoutingKey;
    private final String correoOmitidoRoutingKey;

    public EventoDelPedidoAuditoriaPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${rabbitmq.exchange.eventos-del-pedido-topic}") String eventosDelPedidoTopicExchangeName,
            @Value("${rabbitmq.routing-key.pedido-envio-registrado}") String envioRegistradoRoutingKey,
            @Value("${rabbitmq.routing-key.pedido-correo-enviado}") String correoEnviadoRoutingKey,
            @Value("${rabbitmq.routing-key.pedido-correo-omitido}") String correoOmitidoRoutingKey
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.eventosDelPedidoTopicExchangeName = eventosDelPedidoTopicExchangeName;
        this.envioRegistradoRoutingKey = envioRegistradoRoutingKey;
        this.correoEnviadoRoutingKey = correoEnviadoRoutingKey;
        this.correoOmitidoRoutingKey = correoOmitidoRoutingKey;
    }

    public void publicarEnvioRegistrado(UUID pedidoId, String numeroPedido) {
        publicar(pedidoId, numeroPedido, envioRegistradoRoutingKey);
    }

    public void publicarCorreoEnviado(UUID pedidoId, String numeroPedido) {
        publicar(pedidoId, numeroPedido, correoEnviadoRoutingKey);
    }

    public void publicarCorreoOmitido(UUID pedidoId, String numeroPedido) {
        publicar(pedidoId, numeroPedido, correoOmitidoRoutingKey);
    }

    private void publicar(UUID pedidoId, String numeroPedido, String routingKey) {
        rabbitTemplate.convertAndSend(eventosDelPedidoTopicExchangeName, routingKey,
                new EventoDelPedidoParaAuditoriaEvent(pedidoId, numeroPedido, routingKey));
    }
}

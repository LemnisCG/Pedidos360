package cl.duoc.pedidos360.envio.publisher;

import cl.duoc.pedidos360.envio.event.NotificacionCorreoEnviadaEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Publica en pedidos.exchange que el correo de un pedido ya fue enviado. */
@Component
public class NotificacionCorreoEnviadaPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final String exchangeName;
    private final String routingKey;

    public NotificacionCorreoEnviadaPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${rabbitmq.exchange.pedidos}") String exchangeName,
            @Value("${rabbitmq.routing-key.notificacion-correo-enviada}") String routingKey
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchangeName = exchangeName;
        this.routingKey = routingKey;
    }

    public void publicarCorreoEnviado(NotificacionCorreoEnviadaEvent event) {
        rabbitTemplate.convertAndSend(exchangeName, routingKey, event);
    }
}

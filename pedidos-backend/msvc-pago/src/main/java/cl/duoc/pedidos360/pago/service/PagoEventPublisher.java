package cl.duoc.pedidos360.pago.service;

import cl.duoc.pedidos360.pago.event.PagoCompletadoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Publica eventos de pago completado en el exchange de RabbitMQ.
 * Mantiene la lógica de mensajería separada del controlador.
 */
@Service
public class PagoEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PagoEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchangeName;
    private final String routingKey;

    public PagoEventPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${rabbitmq.exchange.pedidos}") String exchangeName,
            @Value("${rabbitmq.routing-key.pago-completado}") String routingKey
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchangeName = exchangeName;
        this.routingKey = routingKey;
    }

    public void publicarPagoCompletado(PagoCompletadoEvent event) {
        log.info("Publicando evento pago.completado para pedido: {}", event.numeroPedido());
        rabbitTemplate.convertAndSend(exchangeName, routingKey, event);
        log.info("Evento publicado exitosamente para pedido: {}", event.numeroPedido());
    }
}

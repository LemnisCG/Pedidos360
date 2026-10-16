package cl.duoc.pedidos360.envio.publisher;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static cl.duoc.pedidos360.envio.config.RabbitMQConfig.DEFAULT_EXCHANGE;

/**
 * Publica en la cola de reintento de un caso de uso los eventos que fallaron por un error recuperable.
 * Mantiene la mensajería separada de los consumidores: estos solo deciden si corresponde reintentar.
 */
@Component
public class ReintentoDeMensajesPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final String headerNumeroReintento;

    public ReintentoDeMensajesPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${rabbitmq.reintento.header-numero-reintento}") String headerNumeroReintento
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.headerNumeroReintento = headerNumeroReintento;
    }

    /** El número de reintento viaja en un header para que el consumidor sepa cuántos lleva. */
    public void publicarParaReintento(String reintentoQueueName, Object event, int numeroReintento) {
        rabbitTemplate.convertAndSend(DEFAULT_EXCHANGE, reintentoQueueName, event, message -> {
            message.getMessageProperties().setHeader(headerNumeroReintento, numeroReintento);
            return message;
        });
    }
}

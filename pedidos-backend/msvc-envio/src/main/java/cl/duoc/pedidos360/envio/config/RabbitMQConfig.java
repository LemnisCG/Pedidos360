package cl.duoc.pedidos360.envio.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de RabbitMQ para el consumidor.
 * Declara la cola principal, la DLQ (Dead Letter Queue), el exchange y los bindings.
 * Al arrancar, Spring AMQP crea automáticamente estos recursos en RabbitMQ.
 */
@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange.pedidos}")
    private String exchangeName;

    @Value("${rabbitmq.queue.envio}")
    private String queueName;

    @Value("${rabbitmq.queue.envio-dlq}")
    private String dlqName;

    @Value("${rabbitmq.routing-key.pago-completado}")
    private String routingKey;

    /** Exchange donde msvc-pago publica los eventos. */
    @Bean
    public DirectExchange pedidosExchange() {
        return new DirectExchange(exchangeName, true, false);
    }

    /**
     * Cola principal con Dead Letter Exchange configurado.
     * Si un mensaje falla (basicNack con requeue=false), se reenvía automáticamente a la DLQ.
     */
    @Bean
    public Queue envioQueue() {
        return QueueBuilder.durable(queueName)
                .deadLetterExchange("")
                .deadLetterRoutingKey(dlqName)
                .build();
    }

    /** Cola donde terminan los mensajes que fallaron y no se re-encolaron. */
    @Bean
    public Queue envioDlq() {
        return QueueBuilder.durable(dlqName).build();
    }

    /** Vincula la cola principal al exchange con la routing key del evento de pago. */
    @Bean
    public Binding envioBinding(Queue envioQueue, DirectExchange pedidosExchange) {
        return BindingBuilder.bind(envioQueue).to(pedidosExchange).with(routingKey);
    }

    /** Deserializa los mensajes JSON publicados por el productor. */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

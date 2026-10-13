package cl.duoc.pedidos360.pago.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de RabbitMQ para el productor.
 * Solo declara el exchange; las colas y bindings son responsabilidad del consumidor.
 */
@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange.pedidos}")
    private String exchangeName;

    @Bean
    public DirectExchange pedidosExchange() {
        return new DirectExchange(exchangeName, true, false);
    }

    /** Serializa los eventos como JSON para que sean legibles y desacoplados del lenguaje. */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

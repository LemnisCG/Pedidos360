package cl.duoc.pedidos360.pago.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de RabbitMQ compartida por msvc-pago: el exchange y los conversores.
 * Como productor, msvc-pago publica pago.completado; las colas de ese evento son responsabilidad de msvc-envio.
 * Como consumidor, sus colas están en NotificacionCorreoEnviadaMessagingConfig.
 */
@Configuration
public class RabbitMQConfig {

    /** Exchange por defecto de RabbitMQ: entrega directo a la cola cuyo nombre coincide con la routing key. */
    public static final String DEFAULT_EXCHANGE = "";

    public static final String RAW_MESSAGE_CONVERTER_BEAN_NAME = "rawMessageConverter";

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

    /**
     * Conversor sin transformación para el consumidor de la DLQ, que debe poder leer también mensajes con JSON inválido.
     * No es candidato por defecto para que el resto de la aplicación siga usando el conversor JSON.
     */
    @Bean(name = RAW_MESSAGE_CONVERTER_BEAN_NAME, defaultCandidate = false)
    public MessageConverter rawMessageConverter() {
        return new SimpleMessageConverter();
    }
}

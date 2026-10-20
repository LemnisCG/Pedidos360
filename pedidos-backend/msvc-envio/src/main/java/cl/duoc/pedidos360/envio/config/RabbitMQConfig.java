package cl.duoc.pedidos360.envio.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de RabbitMQ compartida por los casos de uso de msvc-envio: el exchange y los conversores.
 * Las colas y bindings de cada caso de uso están en su propia clase de configuración:
 *   - RegistroEnvioMessagingConfig: registrar el envío de cada pago completado.
 *   - NotificacionCorreoMessagingConfig: enviar el correo de confirmación de cada pago completado.
 */
@Configuration
public class RabbitMQConfig {

    /** Exchange por defecto de RabbitMQ: entrega directo a la cola cuyo nombre coincide con la routing key. */
    public static final String DEFAULT_EXCHANGE = "";

    public static final String RAW_MESSAGE_CONVERTER_BEAN_NAME = "rawMessageConverter";

    @Value("${rabbitmq.exchange.pedidos}")
    private String exchangeName;

    /** Exchange donde msvc-pago publica los eventos y donde msvc-envio publica la confirmación del correo. */
    @Bean
    public DirectExchange pedidosExchange() {
        return new DirectExchange(exchangeName, true, false);
    }

    /** Serializa y deserializa los eventos como JSON. */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Conversor sin transformación para los consumidores de las DLQ, que deben poder leer también mensajes con JSON inválido.
     * No es candidato por defecto para que el resto de la aplicación siga usando el conversor JSON.
     */
    @Bean(name = RAW_MESSAGE_CONVERTER_BEAN_NAME, defaultCandidate = false)
    public MessageConverter rawMessageConverter() {
        return new SimpleMessageConverter();
    }
}

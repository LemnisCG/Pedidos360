package cl.duoc.pedidos360.pago.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static cl.duoc.pedidos360.pago.config.RabbitMQConfig.DEFAULT_EXCHANGE;

/**
 * Caso de uso 3: marcar el pedido cuando msvc-envio confirma que salió el correo de confirmación.
 *
 * Ruta de un mensaje:
 *   pedidos.exchange --(notificacion.correo.enviada)--> pago.correo-enviado.queue
 *   pago.correo-enviado.queue --(error recuperable)--> pago.correo-enviado.reintento.queue --(espera TTL)--> pago.correo-enviado.queue
 *   pago.correo-enviado.queue --(error no recuperable o reintentos agotados)--> pago.correo-enviado.dlq
 */
@Configuration
public class NotificacionCorreoEnviadaMessagingConfig {

    @Value("${rabbitmq.queue.correo-enviado}")
    private String queueName;

    @Value("${rabbitmq.queue.correo-enviado-reintento}")
    private String reintentoQueueName;

    @Value("${rabbitmq.queue.correo-enviado-dlq}")
    private String dlqName;

    @Value("${rabbitmq.routing-key.notificacion-correo-enviada}")
    private String routingKey;

    @Value("${rabbitmq.reintento.espera-milisegundos}")
    private int reintentoEsperaMilisegundos;

    /** Cola principal; sus mensajes muertos van a pago.correo-enviado.dlq. */
    @Bean
    public Queue correoEnviadoQueue() {
        return QueueBuilder.durable(queueName)
                .deadLetterExchange(DEFAULT_EXCHANGE)
                .deadLetterRoutingKey(dlqName)
                .build();
    }

    /** Cola de espera sin consumidores: al vencer el TTL, el mensaje vuelve a la cola principal. */
    @Bean
    public Queue correoEnviadoReintentoQueue() {
        return QueueBuilder.durable(reintentoQueueName)
                .ttl(reintentoEsperaMilisegundos)
                .deadLetterExchange(DEFAULT_EXCHANGE)
                .deadLetterRoutingKey(queueName)
                .build();
    }

    /** Cola donde terminan los avisos que no se pudieron aplicar a un pedido. */
    @Bean
    public Queue correoEnviadoDlq() {
        return QueueBuilder.durable(dlqName).build();
    }

    @Bean
    public Binding correoEnviadoBinding(Queue correoEnviadoQueue, DirectExchange pedidosExchange) {
        return BindingBuilder.bind(correoEnviadoQueue).to(pedidosExchange).with(routingKey);
    }
}

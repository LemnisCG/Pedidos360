package cl.duoc.pedidos360.envio.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static cl.duoc.pedidos360.envio.config.RabbitMQConfig.DEFAULT_EXCHANGE;

/**
 * Caso de uso 2: enviar el correo de confirmación de cada pago completado.
 * Recibe una copia del mismo evento que el registro de envío (publish/subscribe): un correo que falla
 * se reintenta por su cuenta sin afectar al registro del envío.
 *
 * Ruta de un mensaje:
 *   pedidos.exchange --(pago.completado)--> notificacion.correo.queue
 *   notificacion.correo.queue --(error recuperable)--> notificacion.correo.reintento.queue --(espera TTL)--> notificacion.correo.queue
 *   notificacion.correo.queue --(error no recuperable o reintentos agotados)--> notificacion.correo.dlq
 *   correo enviado --> pedidos.exchange --(notificacion.correo.enviada)--> msvc-pago
 */
@Configuration
public class NotificacionCorreoMessagingConfig {

    @Value("${rabbitmq.queue.notificacion-correo}")
    private String queueName;

    @Value("${rabbitmq.queue.notificacion-correo-reintento}")
    private String reintentoQueueName;

    @Value("${rabbitmq.queue.notificacion-correo-dlq}")
    private String dlqName;

    @Value("${rabbitmq.routing-key.pago-completado}")
    private String pagoCompletadoRoutingKey;

    @Value("${rabbitmq.reintento.espera-milisegundos}")
    private int reintentoEsperaMilisegundos;

    /** Cola principal de notificaciones; sus mensajes muertos van a notificacion.correo.dlq. */
    @Bean
    public Queue notificacionCorreoQueue() {
        return QueueBuilder.durable(queueName)
                .deadLetterExchange(DEFAULT_EXCHANGE)
                .deadLetterRoutingKey(dlqName)
                .build();
    }

    /** Cola de espera sin consumidores: al vencer el TTL, el mensaje vuelve a la cola de notificaciones. */
    @Bean
    public Queue notificacionCorreoReintentoQueue() {
        return QueueBuilder.durable(reintentoQueueName)
                .ttl(reintentoEsperaMilisegundos)
                .deadLetterExchange(DEFAULT_EXCHANGE)
                .deadLetterRoutingKey(queueName)
                .build();
    }

    /** Cola donde terminan los correos que no se pudieron enviar. */
    @Bean
    public Queue notificacionCorreoDlq() {
        return QueueBuilder.durable(dlqName).build();
    }

    /** Misma routing key que el registro de envío: el exchange direct entrega una copia a cada cola. */
    @Bean
    public Binding notificacionCorreoBinding(Queue notificacionCorreoQueue, DirectExchange pedidosExchange) {
        return BindingBuilder.bind(notificacionCorreoQueue).to(pedidosExchange).with(pagoCompletadoRoutingKey);
    }
}

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
 * Caso de uso 1: registrar el envío de cada pago completado.
 *
 * Ruta de un mensaje:
 *   pedidos.exchange --(pago.completado)--> envio.queue
 *   envio.queue --(error recuperable)--> envio.reintento.queue --(espera TTL)--> envio.queue
 *   envio.queue --(error no recuperable o reintentos agotados)--> envio.dlq
 */
@Configuration
public class RegistroEnvioMessagingConfig {

    @Value("${rabbitmq.queue.envio}")
    private String queueName;

    @Value("${rabbitmq.queue.envio-reintento}")
    private String reintentoQueueName;

    @Value("${rabbitmq.queue.envio-dlq}")
    private String dlqName;

    @Value("${rabbitmq.routing-key.pago-completado}")
    private String pagoCompletadoRoutingKey;

    @Value("${rabbitmq.reintento.espera-milisegundos}")
    private int reintentoEsperaMilisegundos;

    /**
     * Cola principal con Dead Letter Exchange configurado.
     * Si un mensaje falla (basicNack con requeue=false), se reenvía automáticamente a la DLQ.
     */
    @Bean
    public Queue envioQueue() {
        return QueueBuilder.durable(queueName)
                .deadLetterExchange(DEFAULT_EXCHANGE)
                .deadLetterRoutingKey(dlqName)
                .build();
    }

    /**
     * Cola de espera para los reintentos de errores recuperables.
     * No tiene consumidores: cuando vence el TTL, RabbitMQ devuelve el mensaje a la cola principal.
     */
    @Bean
    public Queue envioReintentoQueue() {
        return QueueBuilder.durable(reintentoQueueName)
                .ttl(reintentoEsperaMilisegundos)
                .deadLetterExchange(DEFAULT_EXCHANGE)
                .deadLetterRoutingKey(queueName)
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
        return BindingBuilder.bind(envioQueue).to(pedidosExchange).with(pagoCompletadoRoutingKey);
    }
}

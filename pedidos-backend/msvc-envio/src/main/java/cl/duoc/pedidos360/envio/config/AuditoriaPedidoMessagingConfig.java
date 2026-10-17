package cl.duoc.pedidos360.envio.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static cl.duoc.pedidos360.envio.config.RabbitMQConfig.DEFAULT_EXCHANGE;

/**
 * Caso de uso 4: guardar el historial de auditoría de cada evento del pedido.
 * Usa un exchange topic: la cola se liga con el patrón pedido.# y recibe cualquier evento del pedido
 * sin conocer cada routing key. Un servicio nuevo que publique pedido.algo queda auditado sin tocar esta clase.
 *
 * Ruta de un mensaje:
 *   msvc-pago  --(pedido.pagado)----------->|
 *   msvc-envio --(pedido.envio.registrado)->|  pedidos.eventos.topic --(pedido.#)--> auditoria.pedido.queue
 *   msvc-envio --(pedido.correo.enviado o pedido.correo.omitido)->|
 *   auditoria.pedido.queue --(error recuperable)--> auditoria.pedido.reintento.queue --(espera TTL)--> auditoria.pedido.queue
 *   auditoria.pedido.queue --(error no recuperable o reintentos agotados)--> auditoria.pedido.dlq
 */
@Configuration
public class AuditoriaPedidoMessagingConfig {

    @Value("${rabbitmq.exchange.eventos-del-pedido-topic}")
    private String eventosDelPedidoTopicExchangeName;

    @Value("${rabbitmq.queue.auditoria-pedido}")
    private String queueName;

    @Value("${rabbitmq.queue.auditoria-pedido-reintento}")
    private String reintentoQueueName;

    @Value("${rabbitmq.queue.auditoria-pedido-dlq}")
    private String dlqName;

    @Value("${rabbitmq.binding-pattern.auditoria-pedido-todos-los-eventos}")
    private String patronTodosLosEventosDelPedido;

    @Value("${rabbitmq.reintento.espera-milisegundos}")
    private int reintentoEsperaMilisegundos;

    /** Exchange topic durable; msvc-pago declara el mismo exchange con los mismos argumentos. */
    @Bean
    public TopicExchange eventosDelPedidoTopicExchange() {
        return new TopicExchange(eventosDelPedidoTopicExchangeName, true, false);
    }

    /** Cola principal de auditoría; sus mensajes muertos van a auditoria.pedido.dlq. */
    @Bean
    public Queue auditoriaPedidoQueue() {
        return QueueBuilder.durable(queueName)
                .deadLetterExchange(DEFAULT_EXCHANGE)
                .deadLetterRoutingKey(dlqName)
                .build();
    }

    /** Cola de espera sin consumidores: al vencer el TTL, el mensaje vuelve a la cola de auditoría. */
    @Bean
    public Queue auditoriaPedidoReintentoQueue() {
        return QueueBuilder.durable(reintentoQueueName)
                .ttl(reintentoEsperaMilisegundos)
                .deadLetterExchange(DEFAULT_EXCHANGE)
                .deadLetterRoutingKey(queueName)
                .build();
    }

    /** Cola donde terminan los eventos de auditoría que no se pudieron guardar. */
    @Bean
    public Queue auditoriaPedidoDlq() {
        return QueueBuilder.durable(dlqName).build();
    }

    /** Liga la cola con el patrón pedido.#: recibe pedido.pagado, pedido.envio.registrado, pedido.correo.*, etc. */
    @Bean
    public Binding auditoriaPedidoBinding(Queue auditoriaPedidoQueue, TopicExchange eventosDelPedidoTopicExchange) {
        return BindingBuilder.bind(auditoriaPedidoQueue).to(eventosDelPedidoTopicExchange).with(patronTodosLosEventosDelPedido);
    }
}

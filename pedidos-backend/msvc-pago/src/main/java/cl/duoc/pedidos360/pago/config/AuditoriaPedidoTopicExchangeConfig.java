package cl.duoc.pedidos360.pago.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Caso de uso 4 (lado productor): msvc-pago publica pedido.pagado en el exchange topic de eventos del pedido.
 * La cola de auditoría y su binding (pedido.#) son responsabilidad de msvc-envio. Aquí solo se declara el exchange,
 * con los mismos argumentos que en msvc-envio, para que cada servicio pueda arrancar sin esperar al otro.
 */
@Configuration
public class AuditoriaPedidoTopicExchangeConfig {

    @Value("${rabbitmq.exchange.eventos-del-pedido-topic}")
    private String eventosDelPedidoTopicExchangeName;

    @Bean
    public TopicExchange eventosDelPedidoTopicExchange() {
        return new TopicExchange(eventosDelPedidoTopicExchangeName, true, false);
    }
}

package cl.duoc.pedidos360.pago.service;

import cl.duoc.pedidos360.pago.event.PagoCompletadoEvent;
import cl.duoc.pedidos360.pago.exception.PublicacionDeEventoFallidaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Publica eventos de pago completado en el exchange de RabbitMQ.
 * Mantiene la lógica de mensajería separada del controlador.
 *
 * Espera la confirmación del broker (publisher confirms): si RabbitMQ no confirma, o el mensaje no llega a ninguna
 * cola (mandatory), lanza PublicacionDeEventoFallidaException en vez de perder el evento en silencio.
 */
@Service
public class PagoEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PagoEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchangeName;
    private final String routingKey;
    private final long esperaConfirmacionMilisegundos;

    public PagoEventPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${rabbitmq.exchange.pedidos}") String exchangeName,
            @Value("${rabbitmq.routing-key.pago-completado}") String routingKey,
            @Value("${rabbitmq.publicacion.espera-confirmacion-milisegundos}") long esperaConfirmacionMilisegundos
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchangeName = exchangeName;
        this.routingKey = routingKey;
        this.esperaConfirmacionMilisegundos = esperaConfirmacionMilisegundos;
    }

    public void publicarPagoCompletado(PagoCompletadoEvent event) {
        log.info("Publicando evento pago.completado para pedido: {}", event.numeroPedido());
        CorrelationData correlationData = new CorrelationData(event.pedidoId().toString());
        rabbitTemplate.convertAndSend(exchangeName, routingKey, event, correlationData);

        CorrelationData.Confirm confirmacion = esperarConfirmacion(correlationData, event.numeroPedido());
        if (correlationData.getReturned() != null) {
            throw new PublicacionDeEventoFallidaException("Ninguna cola recibió el evento del pedido "
                    + event.numeroPedido() + ": " + correlationData.getReturned().getReplyText());
        }
        if (!confirmacion.ack()) {
            throw new PublicacionDeEventoFallidaException("RabbitMQ rechazó el evento del pedido "
                    + event.numeroPedido() + ": " + confirmacion.reason());
        }
        log.info("Evento confirmado por RabbitMQ para pedido: {}", event.numeroPedido());
    }

    private CorrelationData.Confirm esperarConfirmacion(CorrelationData correlationData, String numeroPedido) {
        try {
            return correlationData.getFuture().get(esperaConfirmacionMilisegundos, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PublicacionDeEventoFallidaException("Se interrumpió la espera de confirmación del pedido " + numeroPedido, e);
        } catch (ExecutionException | TimeoutException e) {
            throw new PublicacionDeEventoFallidaException("RabbitMQ no confirmó el evento del pedido " + numeroPedido, e);
        }
    }
}

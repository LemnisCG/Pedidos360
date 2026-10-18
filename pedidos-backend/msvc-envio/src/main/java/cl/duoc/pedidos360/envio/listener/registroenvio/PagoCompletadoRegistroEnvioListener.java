package cl.duoc.pedidos360.envio.listener.registroenvio;

import cl.duoc.pedidos360.envio.event.PagoCompletadoEvent;
import cl.duoc.pedidos360.envio.listener.ConsumidorConReintentosYDeadLetterQueue;
import cl.duoc.pedidos360.envio.model.Envio;
import cl.duoc.pedidos360.envio.publisher.EventoDelPedidoAuditoriaPublisher;
import cl.duoc.pedidos360.envio.publisher.ReintentoDeMensajesPublisher;
import cl.duoc.pedidos360.envio.repository.EnvioRepository;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Component;
import org.springframework.transaction.CannotCreateTransactionException;

import java.io.IOException;
import java.util.Map;

/**
 * Caso de uso 1: registra el envío de cada pago completado.
 *
 * Errores:
 *   - Recuperables: fallas temporales de la base de datos (no responde, timeout). Se reintentan con espera.
 *   - No recuperables: evento sin los datos obligatorios. Van directo a la DLQ.
 * Es idempotente: si el pedido ya tiene un envío registrado, confirma el mensaje sin crear otro.
 */
@Component
public class PagoCompletadoRegistroEnvioListener extends ConsumidorConReintentosYDeadLetterQueue<PagoCompletadoEvent> {

    private final EnvioRepository envioRepository;
    private final EventoDelPedidoAuditoriaPublisher eventoDelPedidoAuditoriaPublisher;

    public PagoCompletadoRegistroEnvioListener(
            EnvioRepository envioRepository,
            EventoDelPedidoAuditoriaPublisher eventoDelPedidoAuditoriaPublisher,
            ReintentoDeMensajesPublisher reintentoPublisher,
            @Value("${rabbitmq.queue.envio-reintento}") String reintentoQueueName,
            @Value("${rabbitmq.reintento.maximo-reintentos}") int maximoReintentos,
            @Value("${rabbitmq.reintento.header-numero-reintento}") String headerNumeroReintento
    ) {
        super(reintentoPublisher, reintentoQueueName, maximoReintentos, headerNumeroReintento);
        this.envioRepository = envioRepository;
        this.eventoDelPedidoAuditoriaPublisher = eventoDelPedidoAuditoriaPublisher;
    }

    @RabbitListener(queues = "${rabbitmq.queue.envio}")
    public void onPagoCompletado(
            PagoCompletadoEvent event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
            @Headers Map<String, Object> headers
    ) throws IOException {
        procesarConConfirmacionManual(event, channel, deliveryTag, headers);
    }

    @Override
    protected void procesar(PagoCompletadoEvent event) {
        validarEvento(event);

        if (envioRepository.existsByPedidoId(event.pedidoId())) {
            log.info("El pedido {} ya tiene un envío registrado; mensaje duplicado", event.numeroPedido());
            return;
        }

        Envio envio = new Envio();
        envio.setPedidoId(event.pedidoId());
        envio.setNumeroPedido(event.numeroPedido());
        envio.setUsuarioId(event.usuarioId());
        envio.setEmail(event.email());
        envio.setDireccion(event.direccion());
        envio.setEstado("PREPARANDO");
        envio.setEmailEnviado(false);
        envioRepository.save(envio);
        auditarEnvioRegistrado(event);
    }

    /** El envío ya quedó guardado: si la auditoría falla, se registra en logs pero no se reintenta el registro. */
    private void auditarEnvioRegistrado(PagoCompletadoEvent event) {
        try {
            eventoDelPedidoAuditoriaPublisher.publicarEnvioRegistrado(event.pedidoId(), event.numeroPedido());
        } catch (AmqpException e) {
            log.error("No se pudo auditar el registro del envío del pedido {}: {}", event.numeroPedido(), e.getMessage());
        }
    }

    /** Son recuperables las fallas temporales de infraestructura; el resto se repetiría igual en cada intento. */
    @Override
    protected boolean esErrorRecuperable(Exception e) {
        return e instanceof TransientDataAccessException
                || e instanceof DataAccessResourceFailureException
                || e instanceof CannotCreateTransactionException;
    }

    @Override
    protected String describir(PagoCompletadoEvent event) {
        return event.numeroPedido();
    }

    /** Un evento sin los datos obligatorios nunca podrá procesarse: es un error no recuperable. */
    private void validarEvento(PagoCompletadoEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("El evento de pago completado viene vacío");
        }
        if (event.pedidoId() == null || event.usuarioId() == null) {
            throw new IllegalArgumentException("El evento no trae pedidoId o usuarioId");
        }
        if (estaVacio(event.numeroPedido()) || estaVacio(event.email()) || estaVacio(event.direccion())) {
            throw new IllegalArgumentException("El evento no trae numeroPedido, email o direccion");
        }
    }
}

package cl.duoc.pedidos360.envio.listener.auditoriapedido;

import cl.duoc.pedidos360.envio.event.EventoDelPedidoParaAuditoriaEvent;
import cl.duoc.pedidos360.envio.listener.ConsumidorConReintentosYDeadLetterQueue;
import cl.duoc.pedidos360.envio.model.EventoAuditoriaPedido;
import cl.duoc.pedidos360.envio.publisher.ReintentoDeMensajesPublisher;
import cl.duoc.pedidos360.envio.repository.EventoAuditoriaPedidoRepository;
import com.rabbitmq.client.Channel;
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
 * Caso de uso 4: guarda en el historial de auditoría cada evento del pedido que llega por el exchange topic.
 *
 * Errores:
 *   - Recuperables: fallas temporales de la base de datos. Se reintentan con espera.
 *   - No recuperables: evento sin pedidoId, numeroPedido o tipoEvento. Van directo a la DLQ.
 * Es idempotente: si el pedido ya tiene registrado ese tipo de evento, confirma el mensaje sin duplicarlo.
 */
@Component
public class EventoDelPedidoAuditoriaListener extends ConsumidorConReintentosYDeadLetterQueue<EventoDelPedidoParaAuditoriaEvent> {

    private final EventoAuditoriaPedidoRepository eventoAuditoriaPedidoRepository;

    public EventoDelPedidoAuditoriaListener(
            EventoAuditoriaPedidoRepository eventoAuditoriaPedidoRepository,
            ReintentoDeMensajesPublisher reintentoPublisher,
            @Value("${rabbitmq.queue.auditoria-pedido-reintento}") String reintentoQueueName,
            @Value("${rabbitmq.reintento.maximo-reintentos}") int maximoReintentos,
            @Value("${rabbitmq.reintento.header-numero-reintento}") String headerNumeroReintento
    ) {
        super(reintentoPublisher, reintentoQueueName, maximoReintentos, headerNumeroReintento);
        this.eventoAuditoriaPedidoRepository = eventoAuditoriaPedidoRepository;
    }

    @RabbitListener(queues = "${rabbitmq.queue.auditoria-pedido}")
    public void onEventoDelPedido(
            EventoDelPedidoParaAuditoriaEvent event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
            @Headers Map<String, Object> headers
    ) throws IOException {
        procesarConConfirmacionManual(event, channel, deliveryTag, headers);
    }

    @Override
    protected void procesar(EventoDelPedidoParaAuditoriaEvent event) {
        validarEvento(event);

        if (eventoAuditoriaPedidoRepository.existsByPedidoIdAndTipoEvento(event.pedidoId(), event.tipoEvento())) {
            log.info("El evento {} del pedido {} ya está auditado; mensaje duplicado", event.tipoEvento(), event.numeroPedido());
            return;
        }

        EventoAuditoriaPedido eventoAuditoria = new EventoAuditoriaPedido();
        eventoAuditoria.setPedidoId(event.pedidoId());
        eventoAuditoria.setNumeroPedido(event.numeroPedido());
        eventoAuditoria.setTipoEvento(event.tipoEvento());
        eventoAuditoriaPedidoRepository.save(eventoAuditoria);
        log.info("Evento {} auditado para pedido: {}", event.tipoEvento(), event.numeroPedido());
    }

    /** Son recuperables las fallas temporales de la base de datos; el resto se repetiría igual en cada intento. */
    @Override
    protected boolean esErrorRecuperable(Exception e) {
        return e instanceof TransientDataAccessException
                || e instanceof DataAccessResourceFailureException
                || e instanceof CannotCreateTransactionException;
    }

    @Override
    protected String describir(EventoDelPedidoParaAuditoriaEvent event) {
        return event.numeroPedido() + " (" + event.tipoEvento() + ")";
    }

    /** Sin pedido o sin tipo de evento no hay nada que auditar: es un error no recuperable. */
    private void validarEvento(EventoDelPedidoParaAuditoriaEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("El evento de auditoría viene vacío");
        }
        if (event.pedidoId() == null || estaVacio(event.numeroPedido()) || estaVacio(event.tipoEvento())) {
            throw new IllegalArgumentException("El evento no trae pedidoId, numeroPedido o tipoEvento");
        }
    }
}

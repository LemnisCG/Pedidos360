package cl.duoc.pedidos360.pago.listener.notificacioncorreo;

import cl.duoc.pedidos360.pago.event.NotificacionCorreoEnviadaEvent;
import cl.duoc.pedidos360.pago.exception.PedidoTodaviaNoRegistradoException;
import cl.duoc.pedidos360.pago.listener.ConsumidorConReintentosYDeadLetterQueue;
import cl.duoc.pedidos360.pago.model.Pedido;
import cl.duoc.pedidos360.pago.repository.PedidoRepository;
import cl.duoc.pedidos360.pago.service.ReintentoDeMensajesPublisher;
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
 * Caso de uso 3: marca el pedido con emailEnviado = true cuando msvc-envio confirma que salió el correo.
 *
 * Errores:
 *   - Recuperables: el pedido todavía no está guardado (el aviso llegó antes del fin del checkout) o la base de
 *     datos falla temporalmente. Se reintentan con espera.
 *   - No recuperables: evento sin pedidoId. Van directo a la DLQ.
 * Es idempotente: marcar dos veces el mismo pedido deja el mismo resultado.
 */
@Component
public class NotificacionCorreoEnviadaListener extends ConsumidorConReintentosYDeadLetterQueue<NotificacionCorreoEnviadaEvent> {

    private final PedidoRepository pedidoRepository;

    public NotificacionCorreoEnviadaListener(
            PedidoRepository pedidoRepository,
            ReintentoDeMensajesPublisher reintentoPublisher,
            @Value("${rabbitmq.queue.correo-enviado-reintento}") String reintentoQueueName,
            @Value("${rabbitmq.reintento.maximo-reintentos}") int maximoReintentos,
            @Value("${rabbitmq.reintento.header-numero-reintento}") String headerNumeroReintento
    ) {
        super(reintentoPublisher, reintentoQueueName, maximoReintentos, headerNumeroReintento);
        this.pedidoRepository = pedidoRepository;
    }

    @RabbitListener(queues = "${rabbitmq.queue.correo-enviado}")
    public void onCorreoEnviado(
            NotificacionCorreoEnviadaEvent event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
            @Headers Map<String, Object> headers
    ) throws IOException {
        procesarConConfirmacionManual(event, channel, deliveryTag, headers);
    }

    @Override
    protected void procesar(NotificacionCorreoEnviadaEvent event) {
        if (event == null || event.pedidoId() == null) {
            throw new IllegalArgumentException("El aviso de correo enviado no trae pedidoId");
        }

        Pedido pedido = pedidoRepository.findById(event.pedidoId())
                .orElseThrow(() -> new PedidoTodaviaNoRegistradoException(event.pedidoId()));

        if (pedido.isEmailEnviado()) {
            log.info("El pedido {} ya estaba marcado con el correo enviado; mensaje duplicado", event.numeroPedido());
            return;
        }
        pedido.setEmailEnviado(true);
        pedidoRepository.save(pedido);
    }

    @Override
    protected boolean esErrorRecuperable(Exception e) {
        return e instanceof PedidoTodaviaNoRegistradoException
                || e instanceof TransientDataAccessException
                || e instanceof DataAccessResourceFailureException
                || e instanceof CannotCreateTransactionException;
    }

    @Override
    protected String describir(NotificacionCorreoEnviadaEvent event) {
        return event.numeroPedido();
    }
}

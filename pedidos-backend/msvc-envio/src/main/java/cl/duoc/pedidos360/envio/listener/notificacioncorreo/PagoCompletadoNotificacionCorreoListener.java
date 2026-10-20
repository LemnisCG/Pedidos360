package cl.duoc.pedidos360.envio.listener.notificacioncorreo;

import cl.duoc.pedidos360.envio.event.NotificacionCorreoEnviadaEvent;
import cl.duoc.pedidos360.envio.event.PagoCompletadoEvent;
import cl.duoc.pedidos360.envio.listener.ConsumidorConReintentosYDeadLetterQueue;
import cl.duoc.pedidos360.envio.model.NotificacionCorreo;
import cl.duoc.pedidos360.envio.publisher.EventoDelPedidoAuditoriaPublisher;
import cl.duoc.pedidos360.envio.publisher.NotificacionCorreoEnviadaPublisher;
import cl.duoc.pedidos360.envio.publisher.ReintentoDeMensajesPublisher;
import cl.duoc.pedidos360.envio.repository.NotificacionCorreoRepository;
import cl.duoc.pedidos360.envio.service.CorreoConfirmacionCompraService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.mail.MailSendException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Component;
import org.springframework.transaction.CannotCreateTransactionException;

import java.io.IOException;
import java.util.Map;

/**
 * Caso de uso 2: envía el correo de confirmación de cada pago completado.
 *
 * Errores:
 *   - Recuperables: el servidor SMTP no responde (MailSendException) o la base de datos falla temporalmente.
 *   - No recuperables: evento sin datos obligatorios, credenciales SMTP rechazadas o correo mal construido.
 * Es idempotente: si el pedido ya tiene una notificación registrada, no vuelve a enviar el correo.
 * Si SMTP está deshabilitado, registra la notificación como omitida y confirma el mensaje.
 */
@Component
public class PagoCompletadoNotificacionCorreoListener extends ConsumidorConReintentosYDeadLetterQueue<PagoCompletadoEvent> {

    private final NotificacionCorreoRepository notificacionCorreoRepository;
    private final CorreoConfirmacionCompraService correoConfirmacionCompraService;
    private final NotificacionCorreoEnviadaPublisher notificacionCorreoEnviadaPublisher;
    private final EventoDelPedidoAuditoriaPublisher eventoDelPedidoAuditoriaPublisher;
    private final boolean mailEnabled;

    public PagoCompletadoNotificacionCorreoListener(
            NotificacionCorreoRepository notificacionCorreoRepository,
            CorreoConfirmacionCompraService correoConfirmacionCompraService,
            NotificacionCorreoEnviadaPublisher notificacionCorreoEnviadaPublisher,
            EventoDelPedidoAuditoriaPublisher eventoDelPedidoAuditoriaPublisher,
            ReintentoDeMensajesPublisher reintentoPublisher,
            @Value("${mail.enabled}") boolean mailEnabled,
            @Value("${rabbitmq.queue.notificacion-correo-reintento}") String reintentoQueueName,
            @Value("${rabbitmq.reintento.maximo-reintentos}") int maximoReintentos,
            @Value("${rabbitmq.reintento.header-numero-reintento}") String headerNumeroReintento
    ) {
        super(reintentoPublisher, reintentoQueueName, maximoReintentos, headerNumeroReintento);
        this.notificacionCorreoRepository = notificacionCorreoRepository;
        this.correoConfirmacionCompraService = correoConfirmacionCompraService;
        this.notificacionCorreoEnviadaPublisher = notificacionCorreoEnviadaPublisher;
        this.eventoDelPedidoAuditoriaPublisher = eventoDelPedidoAuditoriaPublisher;
        this.mailEnabled = mailEnabled;
    }

    @RabbitListener(queues = "${rabbitmq.queue.notificacion-correo}")
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

        if (notificacionCorreoRepository.existsByPedidoId(event.pedidoId())) {
            log.info("El pedido {} ya fue notificado; mensaje duplicado", event.numeroPedido());
            return;
        }

        if (!mailEnabled) {
            registrarNotificacion(event, NotificacionCorreo.ESTADO_OMITIDA_SMTP_DESHABILITADO);
            log.info("SMTP deshabilitado: se omite el correo del pedido {}", event.numeroPedido());
            auditarCorreo(event, false);
            return;
        }

        correoConfirmacionCompraService.enviarConfirmacionDeCompra(event);
        registrarNotificacion(event, NotificacionCorreo.ESTADO_ENVIADA);
        log.info("Email enviado exitosamente para pedido: {}", event.numeroPedido());
        avisarCorreoEnviado(event);
        auditarCorreo(event, true);
    }

    /** La notificación ya quedó registrada: si la auditoría falla, se registra en logs pero no se reintenta el correo. */
    private void auditarCorreo(PagoCompletadoEvent event, boolean correoEnviado) {
        try {
            if (correoEnviado) {
                eventoDelPedidoAuditoriaPublisher.publicarCorreoEnviado(event.pedidoId(), event.numeroPedido());
            } else {
                eventoDelPedidoAuditoriaPublisher.publicarCorreoOmitido(event.pedidoId(), event.numeroPedido());
            }
        } catch (AmqpException e) {
            log.error("No se pudo auditar el correo del pedido {}: {}", event.numeroPedido(), e.getMessage());
        }
    }

    /** Son recuperables las fallas temporales del servidor SMTP o de la base de datos. */
    @Override
    protected boolean esErrorRecuperable(Exception e) {
        return e instanceof MailSendException
                || e instanceof TransientDataAccessException
                || e instanceof DataAccessResourceFailureException
                || e instanceof CannotCreateTransactionException;
    }

    @Override
    protected String describir(PagoCompletadoEvent event) {
        return event.numeroPedido();
    }

    private void registrarNotificacion(PagoCompletadoEvent event, String estado) {
        NotificacionCorreo notificacion = new NotificacionCorreo();
        notificacion.setPedidoId(event.pedidoId());
        notificacion.setNumeroPedido(event.numeroPedido());
        notificacion.setEmail(event.email());
        notificacion.setEstado(estado);
        notificacionCorreoRepository.save(notificacion);
    }

    /** El correo ya salió: si el aviso a msvc-pago falla, se registra en logs pero no se reintenta el correo. */
    private void avisarCorreoEnviado(PagoCompletadoEvent event) {
        try {
            notificacionCorreoEnviadaPublisher.publicarCorreoEnviado(
                    new NotificacionCorreoEnviadaEvent(event.pedidoId(), event.numeroPedido()));
        } catch (AmqpException e) {
            log.error("No se pudo avisar a msvc-pago que se envió el correo del pedido {}: {}",
                    event.numeroPedido(), e.getMessage());
        }
    }

    /** Sin pedido, número o correo de destino no hay nada que enviar: es un error no recuperable. */
    private void validarEvento(PagoCompletadoEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("El evento de pago completado viene vacío");
        }
        if (event.pedidoId() == null || estaVacio(event.numeroPedido()) || estaVacio(event.email())) {
            throw new IllegalArgumentException("El evento no trae pedidoId, numeroPedido o email");
        }
    }
}

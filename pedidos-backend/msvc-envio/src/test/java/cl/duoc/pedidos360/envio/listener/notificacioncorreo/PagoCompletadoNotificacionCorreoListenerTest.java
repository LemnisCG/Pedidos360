package cl.duoc.pedidos360.envio.listener.notificacioncorreo;

import cl.duoc.pedidos360.envio.event.NotificacionCorreoEnviadaEvent;
import cl.duoc.pedidos360.envio.event.PagoCompletadoEvent;
import cl.duoc.pedidos360.envio.model.NotificacionCorreo;
import cl.duoc.pedidos360.envio.publisher.NotificacionCorreoEnviadaPublisher;
import cl.duoc.pedidos360.envio.publisher.ReintentoDeMensajesPublisher;
import cl.duoc.pedidos360.envio.repository.NotificacionCorreoRepository;
import cl.duoc.pedidos360.envio.service.CorreoConfirmacionCompraService;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoCompletadoNotificacionCorreoListenerTest {

    private static final long DELIVERY_TAG = 9L;
    private static final String REINTENTO_QUEUE = "notificacion.correo.reintento.queue";

    @Mock
    private NotificacionCorreoRepository notificacionCorreoRepository;
    @Mock
    private CorreoConfirmacionCompraService correoConfirmacionCompraService;
    @Mock
    private NotificacionCorreoEnviadaPublisher notificacionCorreoEnviadaPublisher;
    @Mock
    private ReintentoDeMensajesPublisher reintentoPublisher;
    @Mock
    private Channel channel;

    @Test
    void conSmtpDeshabilitadoRegistraLaNotificacionComoOmitidaYHaceAck() throws Exception {
        PagoCompletadoEvent event = eventoValido();

        listener(false).onPagoCompletado(event, channel, DELIVERY_TAG, Map.of());

        assertThat(notificacionGuardada().getEstado()).isEqualTo(NotificacionCorreo.ESTADO_OMITIDA_SMTP_DESHABILITADO);
        verify(correoConfirmacionCompraService, never()).enviarConfirmacionDeCompra(any());
        verify(channel).basicAck(DELIVERY_TAG, false);
    }

    @Test
    void correoEnviadoSeRegistraSeAvisaAPagoYHaceAck() throws Exception {
        PagoCompletadoEvent event = eventoValido();

        listener(true).onPagoCompletado(event, channel, DELIVERY_TAG, Map.of());

        verify(correoConfirmacionCompraService).enviarConfirmacionDeCompra(event);
        assertThat(notificacionGuardada().getEstado()).isEqualTo(NotificacionCorreo.ESTADO_ENVIADA);
        verify(notificacionCorreoEnviadaPublisher)
                .publicarCorreoEnviado(new NotificacionCorreoEnviadaEvent(event.pedidoId(), event.numeroPedido()));
        verify(channel).basicAck(DELIVERY_TAG, false);
    }

    @Test
    void servidorSmtpCaidoProgramaReintentoSinRegistrarLaNotificacion() throws Exception {
        PagoCompletadoEvent event = eventoValido();
        doThrow(new MailSendException("timeout")).when(correoConfirmacionCompraService).enviarConfirmacionDeCompra(event);

        listener(true).onPagoCompletado(event, channel, DELIVERY_TAG, Map.of());

        verify(reintentoPublisher).publicarParaReintento(REINTENTO_QUEUE, event, 1);
        verify(notificacionCorreoRepository, never()).save(any());
        verify(channel).basicAck(DELIVERY_TAG, false);
    }

    @Test
    void credencialesSmtpRechazadasNoSeReintentanYVanALaDlq() throws Exception {
        PagoCompletadoEvent event = eventoValido();
        doThrow(new MailAuthenticationException("credenciales inválidas"))
                .when(correoConfirmacionCompraService).enviarConfirmacionDeCompra(event);

        listener(true).onPagoCompletado(event, channel, DELIVERY_TAG, Map.of());

        verify(channel).basicNack(DELIVERY_TAG, false, false);
        verify(reintentoPublisher, never()).publicarParaReintento(anyString(), any(), anyInt());
    }

    @Test
    void pedidoYaNotificadoNoVuelveAEnviarElCorreo() throws Exception {
        PagoCompletadoEvent event = eventoValido();
        when(notificacionCorreoRepository.existsByPedidoId(event.pedidoId())).thenReturn(true);

        listener(true).onPagoCompletado(event, channel, DELIVERY_TAG, Map.of());

        verify(correoConfirmacionCompraService, never()).enviarConfirmacionDeCompra(any());
        verify(channel).basicAck(DELIVERY_TAG, false);
    }

    @Test
    void siFallaElAvisoAPagoElCorreoYaEnviadoSeConfirmaIgual() throws Exception {
        PagoCompletadoEvent event = eventoValido();
        doThrow(new AmqpConnectException(new RuntimeException("broker caído")))
                .when(notificacionCorreoEnviadaPublisher).publicarCorreoEnviado(any());

        listener(true).onPagoCompletado(event, channel, DELIVERY_TAG, Map.of());

        verify(channel).basicAck(DELIVERY_TAG, false);
        verify(channel, never()).basicNack(anyLong(), any(Boolean.class), any(Boolean.class));
    }

    private PagoCompletadoNotificacionCorreoListener listener(boolean mailEnabled) {
        return new PagoCompletadoNotificacionCorreoListener(notificacionCorreoRepository, correoConfirmacionCompraService,
                notificacionCorreoEnviadaPublisher, reintentoPublisher, mailEnabled, REINTENTO_QUEUE, 3, "x-numero-reintento");
    }

    private NotificacionCorreo notificacionGuardada() {
        ArgumentCaptor<NotificacionCorreo> captor = ArgumentCaptor.forClass(NotificacionCorreo.class);
        verify(notificacionCorreoRepository).save(captor.capture());
        return captor.getValue();
    }

    private static PagoCompletadoEvent eventoValido() {
        return new PagoCompletadoEvent(UUID.randomUUID(), "P-2", UUID.randomUUID(), "Ana", "ana@example.com",
                "Calle 1", 1000, List.of());
    }
}

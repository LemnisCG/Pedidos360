package cl.duoc.pedidos360.pago.listener.notificacioncorreo;

import cl.duoc.pedidos360.pago.event.NotificacionCorreoEnviadaEvent;
import cl.duoc.pedidos360.pago.model.Pedido;
import cl.duoc.pedidos360.pago.repository.PedidoRepository;
import cl.duoc.pedidos360.pago.service.ReintentoDeMensajesPublisher;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacionCorreoEnviadaListenerTest {

    private static final long DELIVERY_TAG = 3L;
    private static final String REINTENTO_QUEUE = "pago.correo-enviado.reintento.queue";
    private static final String HEADER_REINTENTO = "x-numero-reintento";

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private ReintentoDeMensajesPublisher reintentoPublisher;
    @Mock
    private Channel channel;

    private NotificacionCorreoEnviadaListener listener;

    @BeforeEach
    void crearListener() {
        listener = new NotificacionCorreoEnviadaListener(pedidoRepository, reintentoPublisher, REINTENTO_QUEUE, 3, HEADER_REINTENTO);
    }

    @Test
    void marcaElPedidoConElCorreoEnviadoYHaceAck() throws Exception {
        UUID pedidoId = UUID.randomUUID();
        Pedido pedido = new Pedido();
        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));

        listener.onCorreoEnviado(new NotificacionCorreoEnviadaEvent(pedidoId, "P-3"), channel, DELIVERY_TAG, Map.of());

        assertThat(pedido.isEmailEnviado()).isTrue();
        verify(pedidoRepository).save(pedido);
        verify(channel).basicAck(DELIVERY_TAG, false);
    }

    @Test
    void pedidoTodaviaNoGuardadoSeReintenta() throws Exception {
        UUID pedidoId = UUID.randomUUID();
        NotificacionCorreoEnviadaEvent event = new NotificacionCorreoEnviadaEvent(pedidoId, "P-3");
        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.empty());

        listener.onCorreoEnviado(event, channel, DELIVERY_TAG, Map.of());

        verify(reintentoPublisher).publicarParaReintento(REINTENTO_QUEUE, event, 1);
        verify(channel).basicAck(DELIVERY_TAG, false);
    }

    @Test
    void pedidoInexistenteTrasLosReintentosVaALaDlq() throws Exception {
        UUID pedidoId = UUID.randomUUID();
        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.empty());

        listener.onCorreoEnviado(new NotificacionCorreoEnviadaEvent(pedidoId, "P-3"), channel, DELIVERY_TAG,
                Map.of(HEADER_REINTENTO, 3));

        verify(channel).basicNack(DELIVERY_TAG, false, false);
    }

    @Test
    void avisoSinPedidoIdVaDirectoALaDlq() throws Exception {
        listener.onCorreoEnviado(new NotificacionCorreoEnviadaEvent(null, "P-3"), channel, DELIVERY_TAG, Map.of());

        verify(channel).basicNack(DELIVERY_TAG, false, false);
        verify(reintentoPublisher, never()).publicarParaReintento(anyString(), any(), anyInt());
    }
}

package cl.duoc.pedidos360.envio.listener.auditoriapedido;

import cl.duoc.pedidos360.envio.event.EventoDelPedidoParaAuditoriaEvent;
import cl.duoc.pedidos360.envio.model.EventoAuditoriaPedido;
import cl.duoc.pedidos360.envio.publisher.ReintentoDeMensajesPublisher;
import cl.duoc.pedidos360.envio.repository.EventoAuditoriaPedidoRepository;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.CannotCreateTransactionException;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventoDelPedidoAuditoriaListenerTest {

    private static final long DELIVERY_TAG = 11L;
    private static final String REINTENTO_QUEUE = "auditoria.pedido.reintento.queue";
    private static final String HEADER_REINTENTO = "x-numero-reintento";

    @Mock
    private EventoAuditoriaPedidoRepository eventoAuditoriaPedidoRepository;
    @Mock
    private ReintentoDeMensajesPublisher reintentoPublisher;
    @Mock
    private Channel channel;

    private EventoDelPedidoAuditoriaListener listener;

    @BeforeEach
    void crearListener() {
        listener = new EventoDelPedidoAuditoriaListener(eventoAuditoriaPedidoRepository, reintentoPublisher,
                REINTENTO_QUEUE, 3, HEADER_REINTENTO);
    }

    @Test
    void eventoValidoSeGuardaConSuTipoYHaceAck() throws Exception {
        EventoDelPedidoParaAuditoriaEvent event = eventoValido();

        listener.onEventoDelPedido(event, channel, DELIVERY_TAG, Map.of());

        ArgumentCaptor<EventoAuditoriaPedido> guardado = ArgumentCaptor.forClass(EventoAuditoriaPedido.class);
        verify(eventoAuditoriaPedidoRepository).save(guardado.capture());
        assertThat(guardado.getValue().getTipoEvento()).isEqualTo("pedido.pagado");
        assertThat(guardado.getValue().getPedidoId()).isEqualTo(event.pedidoId());
        verify(channel).basicAck(DELIVERY_TAG, false);
    }

    @Test
    void eventoYaAuditadoNoSeDuplicaYHaceAck() throws Exception {
        EventoDelPedidoParaAuditoriaEvent event = eventoValido();
        when(eventoAuditoriaPedidoRepository.existsByPedidoIdAndTipoEvento(event.pedidoId(), event.tipoEvento()))
                .thenReturn(true);

        listener.onEventoDelPedido(event, channel, DELIVERY_TAG, Map.of());

        verify(eventoAuditoriaPedidoRepository, never()).save(any());
        verify(channel).basicAck(DELIVERY_TAG, false);
    }

    @Test
    void eventoSinTipoEsNoRecuperableYVaDirectoALaDlq() throws Exception {
        EventoDelPedidoParaAuditoriaEvent sinTipo = new EventoDelPedidoParaAuditoriaEvent(UUID.randomUUID(), "P-1", " ");

        listener.onEventoDelPedido(sinTipo, channel, DELIVERY_TAG, Map.of());

        verify(channel).basicNack(DELIVERY_TAG, false, false);
        verify(eventoAuditoriaPedidoRepository, never()).save(any());
        verify(reintentoPublisher, never()).publicarParaReintento(anyString(), any(), anyInt());
    }

    @Test
    void baseDeDatosCaidaProgramaReintentoYConfirmaElOriginal() throws Exception {
        EventoDelPedidoParaAuditoriaEvent event = eventoValido();
        when(eventoAuditoriaPedidoRepository.save(any())).thenThrow(new CannotCreateTransactionException("base caída"));

        listener.onEventoDelPedido(event, channel, DELIVERY_TAG, Map.of());

        verify(reintentoPublisher).publicarParaReintento(REINTENTO_QUEUE, event, 1);
        verify(channel).basicAck(DELIVERY_TAG, false);
        verify(channel, never()).basicNack(anyLong(), any(Boolean.class), any(Boolean.class));
    }

    @Test
    void reintentosAgotadosEnvianElMensajeALaDlq() throws Exception {
        EventoDelPedidoParaAuditoriaEvent event = eventoValido();
        when(eventoAuditoriaPedidoRepository.save(any())).thenThrow(new CannotCreateTransactionException("base caída"));

        listener.onEventoDelPedido(event, channel, DELIVERY_TAG, Map.of(HEADER_REINTENTO, 3));

        verify(channel).basicNack(DELIVERY_TAG, false, false);
        verify(reintentoPublisher, never()).publicarParaReintento(anyString(), any(), anyInt());
    }

    private static EventoDelPedidoParaAuditoriaEvent eventoValido() {
        return new EventoDelPedidoParaAuditoriaEvent(UUID.randomUUID(), "P-1", "pedido.pagado");
    }
}

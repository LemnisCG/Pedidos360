package cl.duoc.pedidos360.envio.listener.registroenvio;

import cl.duoc.pedidos360.envio.event.PagoCompletadoEvent;
import cl.duoc.pedidos360.envio.model.Envio;
import cl.duoc.pedidos360.envio.publisher.EventoDelPedidoAuditoriaPublisher;
import cl.duoc.pedidos360.envio.publisher.ReintentoDeMensajesPublisher;
import cl.duoc.pedidos360.envio.repository.EnvioRepository;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.transaction.CannotCreateTransactionException;
import cl.duoc.pedidos360.envio.kafka.EnvioKafkaPublisher;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoCompletadoRegistroEnvioListenerTest {

    private static final long DELIVERY_TAG = 7L;
    private static final String REINTENTO_QUEUE = "envio.reintento.queue";
    private static final String HEADER_REINTENTO = "x-numero-reintento";

    @Mock
    private EnvioKafkaPublisher envioKafkaPublisher;

    @Mock
    private EnvioRepository envioRepository;
    @Mock
    private EventoDelPedidoAuditoriaPublisher eventoDelPedidoAuditoriaPublisher;
    @Mock
    private ReintentoDeMensajesPublisher reintentoPublisher;
    @Mock
    private Channel channel;

    private PagoCompletadoRegistroEnvioListener listener;

    @BeforeEach
    void crearListener() {

        listener = new PagoCompletadoRegistroEnvioListener(
                envioRepository,
                envioKafkaPublisher,
                eventoDelPedidoAuditoriaPublisher,
                reintentoPublisher,
                REINTENTO_QUEUE,
                3,
                HEADER_REINTENTO);

    }

    @Test
    void eventoValidoGuardaElEnvioYHaceAck() throws Exception {
        PagoCompletadoEvent event = eventoValido();
        when(envioRepository.existsByPedidoId(event.pedidoId())).thenReturn(false);

        listener.onPagoCompletado(event, channel, DELIVERY_TAG, Map.of());

        verify(envioRepository).save(any(Envio.class));
        verify(eventoDelPedidoAuditoriaPublisher).publicarEnvioRegistrado(event.pedidoId(), event.numeroPedido());
        verify(channel).basicAck(DELIVERY_TAG, false);
        verify(channel, never()).basicNack(anyLong(), any(Boolean.class), any(Boolean.class));
    }

    @Test
    void siFallaLaAuditoriaElEnvioIgualSeConfirmaSinReintentar() throws Exception {
        PagoCompletadoEvent event = eventoValido();
        when(envioRepository.existsByPedidoId(event.pedidoId())).thenReturn(false);
        doThrow(new AmqpConnectException(new RuntimeException("broker caído")))
                .when(eventoDelPedidoAuditoriaPublisher)
                .publicarEnvioRegistrado(event.pedidoId(), event.numeroPedido());

        listener.onPagoCompletado(event, channel, DELIVERY_TAG, Map.of());

        verify(channel).basicAck(DELIVERY_TAG, false);
        verify(reintentoPublisher, never()).publicarParaReintento(anyString(), any(), anyInt());
    }

    @Test
    void eventoSinEmailEsNoRecuperableYVaDirectoALaDlq() throws Exception {
        PagoCompletadoEvent sinEmail = new PagoCompletadoEvent(UUID.randomUUID(), "P-1", UUID.randomUUID(),
                "Ana", null, "Calle 1", 1000, List.of());

        listener.onPagoCompletado(sinEmail, channel, DELIVERY_TAG, Map.of());

        verify(channel).basicNack(DELIVERY_TAG, false, false);
        verify(envioRepository, never()).save(any());
        verify(reintentoPublisher, never()).publicarParaReintento(anyString(), any(), anyInt());
    }

    @Test
    void pedidoYaRegistradoNoCreaOtroEnvioYHaceAck() throws Exception {
        PagoCompletadoEvent event = eventoValido();
        when(envioRepository.existsByPedidoId(event.pedidoId())).thenReturn(true);

        listener.onPagoCompletado(event, channel, DELIVERY_TAG, Map.of());

        verify(envioRepository, never()).save(any());
        verify(channel).basicAck(DELIVERY_TAG, false);
    }

    @Test
    void baseDeDatosCaidaProgramaReintentoYConfirmaElOriginal() throws Exception {
        PagoCompletadoEvent event = eventoValido();
        when(envioRepository.save(any())).thenThrow(new CannotCreateTransactionException("base caída"));

        listener.onPagoCompletado(event, channel, DELIVERY_TAG, Map.of());

        verify(reintentoPublisher).publicarParaReintento(REINTENTO_QUEUE, event, 1);
        verify(channel).basicAck(DELIVERY_TAG, false);
        verify(channel, never()).basicNack(anyLong(), any(Boolean.class), any(Boolean.class));
    }

    @Test
    void reintentosAgotadosEnvianElMensajeALaDlq() throws Exception {
        PagoCompletadoEvent event = eventoValido();
        when(envioRepository.save(any())).thenThrow(new CannotCreateTransactionException("base caída"));

        listener.onPagoCompletado(event, channel, DELIVERY_TAG, Map.of(HEADER_REINTENTO, 3));

        verify(channel).basicNack(DELIVERY_TAG, false, false);
        verify(reintentoPublisher, never()).publicarParaReintento(anyString(), any(), anyInt());
    }

    @Test
    void siNoSePuedeProgramarElReintentoElMensajeVaALaDlq() throws Exception {
        PagoCompletadoEvent event = eventoValido();
        when(envioRepository.save(any())).thenThrow(new CannotCreateTransactionException("base caída"));
        doThrow(new AmqpConnectException(new RuntimeException("broker caído")))
                .when(reintentoPublisher).publicarParaReintento(REINTENTO_QUEUE, event, 1);

        listener.onPagoCompletado(event, channel, DELIVERY_TAG, Map.of());

        verify(channel).basicNack(DELIVERY_TAG, false, false);
        verify(channel, never()).basicAck(anyLong(), any(Boolean.class));
    }

    private static PagoCompletadoEvent eventoValido() {
        return new PagoCompletadoEvent(UUID.randomUUID(), "P-1", UUID.randomUUID(), "Ana", "ana@example.com",
                "Calle 1", 1000, List.of(new PagoCompletadoEvent.ItemEvent("Juego", 1, 1000, "/juego.png")));
    }
}

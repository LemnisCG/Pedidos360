package cl.duoc.pedidos360.pago.service;

import cl.duoc.pedidos360.pago.event.PagoCompletadoEvent;
import cl.duoc.pedidos360.pago.exception.PublicacionDeEventoFallidaException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;

@ExtendWith(MockitoExtension.class)
class PagoEventPublisherTest {

    private static final String EXCHANGE = "pedidos.exchange";
    private static final String ROUTING_KEY = "pago.completado";

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Test
    void publicacionConfirmadaPorElBrokerTerminaSinErrores() {
        simularRespuestaDelBroker(correlation -> correlation.getFuture().complete(new CorrelationData.Confirm(true, null)));

        assertThatCode(() -> publisher(1000).publicarPagoCompletado(evento())).doesNotThrowAnyException();
    }

    @Test
    void publicacionRechazadaPorElBrokerLanzaExcepcion() {
        simularRespuestaDelBroker(correlation ->
                correlation.getFuture().complete(new CorrelationData.Confirm(false, "cola llena")));

        assertThatThrownBy(() -> publisher(1000).publicarPagoCompletado(evento()))
                .isInstanceOf(PublicacionDeEventoFallidaException.class)
                .hasMessageContaining("cola llena");
    }

    @Test
    void eventoQueNoLlegaANingunaColaLanzaExcepcion() {
        simularRespuestaDelBroker(correlation -> {
            correlation.setReturned(new ReturnedMessage(new Message(new byte[0]), 312, "NO_ROUTE", EXCHANGE, ROUTING_KEY));
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
        });

        assertThatThrownBy(() -> publisher(1000).publicarPagoCompletado(evento()))
                .isInstanceOf(PublicacionDeEventoFallidaException.class)
                .hasMessageContaining("NO_ROUTE");
    }

    @Test
    void sinConfirmacionDentroDelPlazoLanzaExcepcion() {
        simularRespuestaDelBroker(correlation -> {
            // El broker nunca responde
        });

        assertThatThrownBy(() -> publisher(50).publicarPagoCompletado(evento()))
                .isInstanceOf(PublicacionDeEventoFallidaException.class)
                .hasMessageContaining("no confirmó");
    }

    private void simularRespuestaDelBroker(Consumer<CorrelationData> respuesta) {
        doAnswer(invocation -> {
            respuesta.accept(invocation.getArgument(3));
            return null;
        }).when(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY), any(Object.class), any(CorrelationData.class));
    }

    private PagoEventPublisher publisher(long esperaConfirmacionMilisegundos) {
        return new PagoEventPublisher(rabbitTemplate, EXCHANGE, ROUTING_KEY, esperaConfirmacionMilisegundos);
    }

    private static PagoCompletadoEvent evento() {
        return new PagoCompletadoEvent(UUID.randomUUID(), "P-4", UUID.randomUUID(), "Ana", "ana@example.com",
                "Calle 1", 1000, List.of());
    }
}

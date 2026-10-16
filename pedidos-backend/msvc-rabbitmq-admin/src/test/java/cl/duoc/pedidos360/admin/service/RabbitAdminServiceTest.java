package cl.duoc.pedidos360.admin.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RabbitAdminServiceTest {

    @Mock
    private RabbitAdmin rabbitAdmin;

    @InjectMocks
    private RabbitAdminService rabbitAdminService;

    @Test
    void creaUnaColaDurable() {
        rabbitAdminService.createQueue("demo.cola");

        ArgumentCaptor<Queue> captor = ArgumentCaptor.forClass(Queue.class);
        verify(rabbitAdmin).declareQueue(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("demo.cola");
        assertThat(captor.getValue().isDurable()).isTrue();
    }

    @Test
    void creaUnExchangeDirectoDurable() {
        rabbitAdminService.createExchange("demo.exchange");

        ArgumentCaptor<Exchange> captor = ArgumentCaptor.forClass(Exchange.class);
        verify(rabbitAdmin).declareExchange(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("demo.exchange");
        assertThat(captor.getValue().getType()).isEqualTo("direct");
        assertThat(captor.getValue().isDurable()).isTrue();
    }

    @Test
    void creaYEliminaUnBindingEntreColaYExchange() {
        rabbitAdminService.createBinding("demo.cola", "demo.exchange", "demo.key");
        rabbitAdminService.deleteBinding("demo.cola", "demo.exchange", "demo.key");

        ArgumentCaptor<Binding> creado = ArgumentCaptor.forClass(Binding.class);
        ArgumentCaptor<Binding> eliminado = ArgumentCaptor.forClass(Binding.class);
        verify(rabbitAdmin).declareBinding(creado.capture());
        verify(rabbitAdmin).removeBinding(eliminado.capture());
        for (Binding binding : new Binding[]{creado.getValue(), eliminado.getValue()}) {
            assertThat(binding.getDestination()).isEqualTo("demo.cola");
            assertThat(binding.getExchange()).isEqualTo("demo.exchange");
            assertThat(binding.getRoutingKey()).isEqualTo("demo.key");
        }
    }

    @Test
    void eliminaColasYExchangesPorNombre() {
        rabbitAdminService.deleteQueue("demo.cola");
        rabbitAdminService.deleteExchange("demo.exchange");

        verify(rabbitAdmin).deleteQueue("demo.cola");
        verify(rabbitAdmin).deleteExchange("demo.exchange");
    }
}

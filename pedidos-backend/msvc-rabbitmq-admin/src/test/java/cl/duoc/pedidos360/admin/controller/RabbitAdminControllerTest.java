package cl.duoc.pedidos360.admin.controller;

import cl.duoc.pedidos360.admin.service.RabbitAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpIOException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.io.IOException;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RabbitAdminControllerTest {

    @Mock
    private RabbitAdminService rabbitAdminService;

    private MockMvc mockMvc;

    @BeforeEach
    void crearMockMvc() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new RabbitAdminController(rabbitAdminService))
                .setControllerAdvice(new RabbitAdminApiExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void crearColaValidaResponde201() throws Exception {
        mockMvc.perform(post("/api/admin/queues").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"demo.cola\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Cola creada: demo.cola"));

        verify(rabbitAdminService).createQueue("demo.cola");
    }

    @Test
    void crearColaConNombreVacioResponde400ConElMotivo() throws Exception {
        mockMvc.perform(post("/api/admin/queues").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Datos de entrada inválidos"))
                .andExpect(jsonPath("$.details", hasItem("name: El nombre no puede estar vacío")));

        verify(rabbitAdminService, never()).createQueue(anyString());
    }

    @Test
    void crearColaConCaracteresInvalidosResponde400() throws Exception {
        mockMvc.perform(post("/api/admin/queues").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"cola con espacios\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasItem("name: El nombre contiene caracteres inválidos")));
    }

    @Test
    void bindingIncompletoResponde400ConCadaCampoFaltante() throws Exception {
        mockMvc.perform(post("/api/admin/bindings").contentType(MediaType.APPLICATION_JSON).content("{\"queueName\":\"demo.cola\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasItem("exchangeName: El nombre del exchange no puede estar vacío")))
                .andExpect(jsonPath("$.details", hasItem("routingKey: La routing key no puede estar vacía")));
    }

    @Test
    void eliminarBindingSinParametrosResponde400() throws Exception {
        mockMvc.perform(delete("/api/admin/bindings"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasItem("Falta el parámetro obligatorio: queueName")));
    }

    @Test
    void eliminarColaResponde204() throws Exception {
        mockMvc.perform(delete("/api/admin/queues/demo.cola"))
                .andExpect(status().isNoContent());

        verify(rabbitAdminService).deleteQueue("demo.cola");
    }

    @Test
    void rechazoDeRabbitMqResponde409() throws Exception {
        doThrow(new AmqpIOException(new IOException("NOT_FOUND - no queue 'no.existe'")))
                .when(rabbitAdminService).createBinding("no.existe", "demo.exchange", "demo.key");

        mockMvc.perform(post("/api/admin/bindings").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"queueName\":\"no.existe\",\"exchangeName\":\"demo.exchange\",\"routingKey\":\"demo.key\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("RabbitMQ rechazó la operación"));
    }
}

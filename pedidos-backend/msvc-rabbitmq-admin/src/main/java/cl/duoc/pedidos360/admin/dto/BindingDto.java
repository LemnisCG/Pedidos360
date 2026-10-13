package cl.duoc.pedidos360.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class BindingDto {

    @NotBlank(message = "El nombre de la cola no puede estar vacío")
    private String queueName;

    @NotBlank(message = "El nombre del exchange no puede estar vacío")
    private String exchangeName;

    @NotBlank(message = "La routing key no puede estar vacía")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "La routing key contiene caracteres inválidos")
    private String routingKey;

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public String getExchangeName() {
        return exchangeName;
    }

    public void setExchangeName(String exchangeName) {
        this.exchangeName = exchangeName;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }
}

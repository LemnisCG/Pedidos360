package cl.duoc.pedidos360.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class BindingDto {

    @NotBlank(message = "El nombre de la cola no puede estar vacío")
    @Pattern(regexp = RabbitResourceNameRules.VALID_NAME_PATTERN, message = "El nombre de la cola contiene caracteres inválidos")
    @Size(max = RabbitResourceNameRules.MAX_NAME_LENGTH, message = "El nombre de la cola no puede superar los 255 caracteres")
    private String queueName;

    @NotBlank(message = "El nombre del exchange no puede estar vacío")
    @Pattern(regexp = RabbitResourceNameRules.VALID_NAME_PATTERN, message = "El nombre del exchange contiene caracteres inválidos")
    @Size(max = RabbitResourceNameRules.MAX_NAME_LENGTH, message = "El nombre del exchange no puede superar los 255 caracteres")
    private String exchangeName;

    @NotBlank(message = "La routing key no puede estar vacía")
    @Pattern(regexp = RabbitResourceNameRules.VALID_NAME_PATTERN, message = "La routing key contiene caracteres inválidos")
    @Size(max = RabbitResourceNameRules.MAX_NAME_LENGTH, message = "La routing key no puede superar los 255 caracteres")
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

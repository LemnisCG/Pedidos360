package cl.duoc.pedidos360.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ResourceDto {

    @NotBlank(message = "El nombre no puede estar vacío")
    @Pattern(regexp = RabbitResourceNameRules.VALID_NAME_PATTERN, message = "El nombre contiene caracteres inválidos")
    @Size(max = RabbitResourceNameRules.MAX_NAME_LENGTH, message = "El nombre no puede superar los 255 caracteres")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

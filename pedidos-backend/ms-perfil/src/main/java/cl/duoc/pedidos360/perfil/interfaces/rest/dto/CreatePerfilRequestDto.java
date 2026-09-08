package cl.duoc.pedidos360.perfil.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CreatePerfilRequestDto(
        UUID idProvider,
        String email,
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,
        String apellido,
        String direccionEnvio,
        String telefono
) {
}
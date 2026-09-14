package cl.duoc.pedidos360.perfil.interfaces.rest.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CreatePerfilRequestDto(
        @JsonProperty("idProvider")
        @JsonAlias({"azureOid"})
        UUID idProvider,
        String email,
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,
        String apellido,
        String direccionEnvio,
        String telefono
) {
}
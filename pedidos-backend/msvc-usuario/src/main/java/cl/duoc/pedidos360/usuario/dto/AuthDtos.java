package cl.duoc.pedidos360.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** DTOs agrupados para mantener pequeña la superficie pública del microservicio. */
public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank @Size(max=80) String nombre,
            @NotBlank @Size(max=80) String apellido,
            @NotBlank @Email String email,
            @NotBlank @Size(min=8,max=72) String password) {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    public record UserResponse(UUID id, String nombre, String apellido, String email, String provider) {}

    public record AuthResponse(String token, UserResponse usuario) {}
}

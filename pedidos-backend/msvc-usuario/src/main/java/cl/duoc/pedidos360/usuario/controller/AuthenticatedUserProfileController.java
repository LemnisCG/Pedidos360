package cl.duoc.pedidos360.usuario.controller;

import cl.duoc.pedidos360.usuario.model.Usuario;
import cl.duoc.pedidos360.usuario.service.AuthenticatedUserProfileSynchronizationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Perfil del usuario que inició sesión con Entra External ID.
 * X-User-* son cabeceras internas confiables que agrega api-gateway después de validar el access token.
 */
@RestController
public class AuthenticatedUserProfileController {
    private final AuthenticatedUserProfileSynchronizationService profileSynchronizationService;

    public AuthenticatedUserProfileController(AuthenticatedUserProfileSynchronizationService profileSynchronizationService) {
        this.profileSynchronizationService = profileSynchronizationService;
    }

    @GetMapping("/api/usuarios/me")
    public Usuario synchronizeAndReturnMyProfile(
            @RequestHeader("X-User-Id") UUID entraUserObjectId,
            @RequestHeader(value = "X-User-Email", required = false) String email,
            @RequestHeader(value = "X-User-Name", required = false) String fullName) {
        return profileSynchronizationService.synchronizeProfileFromGatewayIdentity(entraUserObjectId, email, fullName);
    }
}

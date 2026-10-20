package cl.duoc.pedidos360.usuario.service;

import cl.duoc.pedidos360.usuario.model.Usuario;
import cl.duoc.pedidos360.usuario.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

/**
 * Mantiene el perfil local al día con los datos que el api-gateway extrae del access token.
 * La primera vez que un usuario de Entra entra a Pedidos360 se crea su fila; después se actualiza.
 */
@Service
public class AuthenticatedUserProfileSynchronizationService {
    private static final int MAXIMUM_NAME_LENGTH = 80;

    private final UsuarioRepository usuarioRepository;

    public AuthenticatedUserProfileSynchronizationService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public Usuario synchronizeProfileFromGatewayIdentity(UUID entraUserObjectId, String emailFromToken, String fullNameFromToken) {
        String normalizedEmail = emailFromToken == null || emailFromToken.isBlank()
                ? entraUserObjectId + "@sin-correo.pedidos360.local"
                : emailFromToken.trim().toLowerCase(Locale.ROOT);

        Usuario usuario = usuarioRepository.findById(entraUserObjectId).orElse(null);
        if (usuario == null) {
            // Una cuenta del login local anterior con el mismo correo ya no es utilizable y
            // bloquearía la restricción UNIQUE(email): se reemplaza por el perfil de Entra.
            usuarioRepository.findByEmailIgnoreCase(normalizedEmail).ifPresent(legacyLocalAccount -> {
                usuarioRepository.delete(legacyLocalAccount);
                usuarioRepository.flush();
            });
            usuario = new Usuario(entraUserObjectId);
        }

        String[] nameParts = splitFullName(fullNameFromToken, normalizedEmail);
        usuario.setNombre(nameParts[0]);
        usuario.setApellido(nameParts[1]);
        usuario.setEmail(normalizedEmail);
        return usuarioRepository.save(usuario);
    }

    private String[] splitFullName(String fullName, String fallbackEmail) {
        String usableName = fullName == null || fullName.isBlank() || fullName.contains("@")
                ? fallbackEmail.split("@")[0]
                : fullName.trim();
        String[] parts = usableName.split("\\s+", 2);
        return new String[]{truncate(parts[0]), truncate(parts.length > 1 ? parts[1] : "")};
    }

    private String truncate(String value) {
        return value.length() > MAXIMUM_NAME_LENGTH ? value.substring(0, MAXIMUM_NAME_LENGTH) : value;
    }
}

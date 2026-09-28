package cl.duoc.pedidos360.usuario.repository;

import cl.duoc.pedidos360.usuario.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    Optional<Usuario> findByEmailIgnoreCase(String email);
    Optional<Usuario> findByProviderAndProviderUserId(String provider, String providerUserId);
}

package cl.duoc.pedidos360.usuario.repository;

import cl.duoc.pedidos360.usuario.model.OAuthIdentity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Persistencia de vínculos entre usuarios locales e identidades externas. */
public interface OAuthIdentityRepository extends JpaRepository<OAuthIdentity, UUID> {
    Optional<OAuthIdentity> findByProviderAndProviderUserId(String provider, String providerUserId);
    Optional<OAuthIdentity> findByUsuarioIdAndProvider(UUID usuarioId, String provider);
}

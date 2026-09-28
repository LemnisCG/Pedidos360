package cl.duoc.pedidos360.usuario.model;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * Vincula una cuenta local de Pedidos360 con uno o más proveedores OAuth.
 * Se mantiene separado de Usuario para que una misma cuenta pueda usar
 * Google, Facebook, Discord y Microsoft sin sobrescribir el vínculo anterior.
 */
@Entity
@Table(
        name = "oauth_identities",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_oauth_provider_external_id",
                        columnNames = {"provider", "provider_user_id"}),
                @UniqueConstraint(
                        name = "uk_oauth_usuario_provider",
                        columnNames = {"usuario_id", "provider"})
        })
public class OAuthIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(nullable = false, length = 30)
    private String provider;

    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    protected OAuthIdentity() {}

    public OAuthIdentity(UUID usuarioId, String provider, String providerUserId) {
        this.usuarioId = usuarioId;
        this.provider = provider;
        this.providerUserId = providerUserId;
    }

    public UUID getId() { return id; }
    public UUID getUsuarioId() { return usuarioId; }
    public void setUsuarioId(UUID usuarioId) { this.usuarioId = usuarioId; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getProviderUserId() { return providerUserId; }
    public void setProviderUserId(String providerUserId) { this.providerUserId = providerUserId; }
}

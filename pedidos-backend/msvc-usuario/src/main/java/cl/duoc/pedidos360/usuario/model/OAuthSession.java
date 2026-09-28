package cl.duoc.pedidos360.usuario.model;

import jakarta.persistence.*;
import java.time.Instant;

/** Guarda state + code_verifier temporalmente para validar el retorno OAuth/PKCE. */
@Entity
@Table(name = "oauth_sessions")
public class OAuthSession {
    @Id private String state;
    @Column(nullable = false) private String provider;
    @Column(nullable = false, length = 160) private String codeVerifier;
    @Column(nullable = false) private Instant expiresAt;

    protected OAuthSession() {}
    public OAuthSession(String state, String provider, String codeVerifier, Instant expiresAt) {
        this.state = state; this.provider = provider; this.codeVerifier = codeVerifier; this.expiresAt = expiresAt;
    }
    public String getState() { return state; }
    public String getProvider() { return provider; }
    public String getCodeVerifier() { return codeVerifier; }
    public Instant getExpiresAt() { return expiresAt; }
}

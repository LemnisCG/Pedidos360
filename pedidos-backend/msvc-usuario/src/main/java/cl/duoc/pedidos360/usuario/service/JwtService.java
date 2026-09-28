package cl.duoc.pedidos360.usuario.service;

import cl.duoc.pedidos360.usuario.model.Usuario;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/** Emite el JWT interno consumido por api-gateway, independientemente del proveedor de origen. */
@Service
public class JwtService {
    private final SecretKey key;
    private final long minutes;

    public JwtService(@Value("${pedidos360.jwt-secret}") String secret,
                      @Value("${pedidos360.jwt-expiration-minutes}") long minutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.minutes = minutes;
    }

    public String create(Usuario usuario) {
        return create(usuario, usuario.getProvider());
    }

    /** Permite indicar cómo se autenticó esta sesión sin sobrescribir el proveedor vinculado en BD. */
    public String create(Usuario usuario, String authProvider) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(usuario.getId().toString())
                .claim("email", usuario.getEmail())
                .claim("name", usuario.nombreCompleto())
                .claim("provider", authProvider)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(minutes, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }
}

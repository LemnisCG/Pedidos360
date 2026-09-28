package cl.duoc.pedidos360.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Valida el JWT interno emitido por msvc-usuario antes de enrutar rutas privadas.
 * Además agrega cabeceras X-User-* para que los microservicios no deban decodificar el token otra vez.
 */
@Component
@Order(-50)
public class JwtGatewayFilter implements WebFilter {
    private final SecretKey key;

    public JwtGatewayFilter(@Value("${pedidos360.jwt-secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        HttpMethod method = exchange.getRequest().getMethod();

        // Preflight, autenticación y catálogo de lectura permanecen públicos.
        if (HttpMethod.OPTIONS.equals(method) || isPublic(path, method)) {
            return chain.filter(exchange);
        }

        String header = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return unauthorized(exchange, "Token Bearer requerido");
        }

        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(header.substring(7)).getPayload();

            ServerWebExchange enriched = exchange.mutate().request(builder -> builder.headers(headers -> {
                headers.set("X-User-Id", claims.getSubject());
                headers.set("X-User-Email", String.valueOf(claims.get("email")));
                headers.set("X-User-Name", String.valueOf(claims.get("name")));
                headers.set("X-Auth-Provider", String.valueOf(claims.get("provider")));
            })).build();
            return chain.filter(enriched);
        } catch (Exception ex) {
            return unauthorized(exchange, "JWT inválido o expirado");
        }
    }

    private boolean isPublic(String path, HttpMethod method) {
        if (path.equals("/api/auth/login") || path.equals("/api/auth/register")) return true;
        if (path.startsWith("/api/auth/oauth2/")) return true;
        if (path.equals("/api/catalogo") && HttpMethod.GET.equals(method)) return true;
        return path.equals("/health");
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().set("Content-Type", "application/json;charset=UTF-8");
        byte[] body = ("{\"error\":\"" + message.replace("\"", "\\\"") + "\"}").getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }
}

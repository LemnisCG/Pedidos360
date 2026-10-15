package cl.duoc.pedidos360.gateway.security;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Autoriza la petición solo si el token validado trae TODAS las autoridades indicadas
 * (por ejemplo el scope del BFF y, además, el rol Administrador).
 * Sin token responde 401; con token pero sin alguna autoridad responde 403.
 */
public class RequiredGrantedAuthoritiesAuthorizationManager implements ReactiveAuthorizationManager<AuthorizationContext> {

    private final Set<String> requiredAuthorities;

    public RequiredGrantedAuthoritiesAuthorizationManager(String... requiredAuthorities) {
        this.requiredAuthorities = Set.of(requiredAuthorities);
    }

    @Override
    public Mono<AuthorizationResult> authorize(Mono<Authentication> authentication, AuthorizationContext context) {
        return authentication
                .filter(Authentication::isAuthenticated)
                .map(this::hasEveryRequiredAuthority)
                .defaultIfEmpty(false)
                .map(AuthorizationDecision::new);
    }

    private boolean hasEveryRequiredAuthority(Authentication authentication) {
        Set<String> grantedAuthorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        return grantedAuthorities.containsAll(requiredAuthorities);
    }
}

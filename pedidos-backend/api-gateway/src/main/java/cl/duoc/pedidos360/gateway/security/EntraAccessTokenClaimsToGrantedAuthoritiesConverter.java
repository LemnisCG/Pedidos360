package cl.duoc.pedidos360.gateway.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/**
 * Lee del access token de Entra los claims de autorización:
 * "scp" (scopes delegados, separados por espacio) y "roles" (App Roles asignados al usuario).
 */
public class EntraAccessTokenClaimsToGrantedAuthoritiesConverter implements Converter<Jwt, Flux<GrantedAuthority>> {

    public static final String SCOPE_AUTHORITY_PREFIX = "SCOPE_";
    public static final String ROLE_AUTHORITY_PREFIX = "ROLE_";

    private static final String SCOPES_CLAIM = "scp";
    private static final String APP_ROLES_CLAIM = "roles";

    @Override
    public Flux<GrantedAuthority> convert(Jwt accessToken) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        String scopes = accessToken.getClaimAsString(SCOPES_CLAIM);
        if (scopes != null && !scopes.isBlank()) {
            for (String scope : scopes.trim().split("\\s+")) {
                authorities.add(new SimpleGrantedAuthority(SCOPE_AUTHORITY_PREFIX + scope));
            }
        }

        // Un cliente auto-registrado no tiene App Role asignado y su token no trae el claim.
        List<String> appRoles = accessToken.getClaimAsStringList(APP_ROLES_CLAIM);
        if (appRoles != null) {
            for (String appRole : appRoles) {
                authorities.add(new SimpleGrantedAuthority(ROLE_AUTHORITY_PREFIX + appRole));
            }
        }

        return Flux.fromIterable(authorities);
    }
}

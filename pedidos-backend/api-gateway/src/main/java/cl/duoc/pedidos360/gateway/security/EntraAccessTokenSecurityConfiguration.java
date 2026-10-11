package cl.duoc.pedidos360.gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;

/**
 * Convierte al API Gateway en un Resource Server OAuth 2.0 de Microsoft Entra External ID.
 * La firma, el emisor (iss), la audiencia (aud) y la vigencia (exp) se validan con las propiedades
 * spring.security.oauth2.resourceserver.jwt.*; aquí se decide qué scope y qué rol exige cada ruta.
 */
@Configuration
@EnableWebFluxSecurity
public class EntraAccessTokenSecurityConfiguration {

    /** Valor del App Role definido en Entra; llega en el claim "roles". */
    public static final String ADMINISTRADOR_APP_ROLE = "Administrador";

    @Bean
    SecurityWebFilterChain apiGatewaySecurityWebFilterChain(
            ServerHttpSecurity http,
            SecurityErrorJsonResponseWriter securityErrorJsonResponseWriter,
            @Value("${pedidos360.security.required-access-token-scope}") String requiredAccessTokenScope) {

        String requiredScopeAuthority =
                EntraAccessTokenClaimsToGrantedAuthoritiesConverter.SCOPE_AUTHORITY_PREFIX + requiredAccessTokenScope;
        String administradorRoleAuthority =
                EntraAccessTokenClaimsToGrantedAuthoritiesConverter.ROLE_AUTHORITY_PREFIX + ADMINISTRADOR_APP_ROLE;

        ServerAuthenticationEntryPoint respondUnauthorizedWhenTokenIsMissingOrInvalid = (exchange, exception) ->
                securityErrorJsonResponseWriter.write(exchange, HttpStatus.UNAUTHORIZED,
                        exchange.getRequest().getHeaders().getFirst("Authorization") == null
                                ? "Se requiere un access token Bearer de Entra External ID."
                                : "El access token es inválido, expiró o no fue emitido para esta API.");

        ServerAccessDeniedHandler respondForbiddenWhenScopeOrRoleIsMissing = (exchange, exception) ->
                securityErrorJsonResponseWriter.write(exchange, HttpStatus.FORBIDDEN,
                        "El access token no tiene el scope o el rol requerido para esta operación.");

        return http
                // API sin cookies de sesión: el token viaja en la cabecera Authorization, no aplica CSRF.
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(Customizer.withDefaults())
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()
                        .pathMatchers("/health").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/catalogo").permitAll()
                        .pathMatchers(HttpMethod.PUT, "/api/catalogo/banner").access(
                                new RequiredGrantedAuthoritiesAuthorizationManager(
                                        requiredScopeAuthority, administradorRoleAuthority))
                        .pathMatchers("/api/**").access(
                                new RequiredGrantedAuthoritiesAuthorizationManager(requiredScopeAuthority))
                        .anyExchange().denyAll())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(entraAccessTokenAuthenticationConverter()))
                        .authenticationEntryPoint(respondUnauthorizedWhenTokenIsMissingOrInvalid)
                        .accessDeniedHandler(respondForbiddenWhenScopeOrRoleIsMissing))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(respondUnauthorizedWhenTokenIsMissingOrInvalid)
                        .accessDeniedHandler(respondForbiddenWhenScopeOrRoleIsMissing))
                .build();
    }

    private ReactiveJwtAuthenticationConverter entraAccessTokenAuthenticationConverter() {
        ReactiveJwtAuthenticationConverter converter = new ReactiveJwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new EntraAccessTokenClaimsToGrantedAuthoritiesConverter());
        // "oid" identifica al usuario de forma estable en el tenant; "sub" cambia según la aplicación.
        converter.setPrincipalClaimName(AuthenticatedUserIdentityHeaders.ENTRA_USER_OBJECT_ID_CLAIM);
        return converter;
    }
}

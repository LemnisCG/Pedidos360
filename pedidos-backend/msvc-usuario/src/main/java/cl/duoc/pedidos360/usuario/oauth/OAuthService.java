package cl.duoc.pedidos360.usuario.oauth;

import cl.duoc.pedidos360.usuario.model.OAuthIdentity;
import cl.duoc.pedidos360.usuario.model.OAuthSession;
import cl.duoc.pedidos360.usuario.model.Usuario;
import cl.duoc.pedidos360.usuario.repository.OAuthIdentityRepository;
import cl.duoc.pedidos360.usuario.repository.OAuthSessionRepository;
import cl.duoc.pedidos360.usuario.repository.UsuarioRepository;
import cl.duoc.pedidos360.usuario.service.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Collection;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementa Authorization Code + PKCE para los proveedores externos.
 * Después de validar la identidad externa, Pedidos360 emite su propio JWT interno.
 */
@Service
public class OAuthService {
    private static final Logger log = LoggerFactory.getLogger(OAuthService.class);

    private final OAuthProperties properties;
    private final OAuthSessionRepository sessions;
    private final OAuthIdentityRepository identities;
    private final UsuarioRepository users;
    private final JwtService jwtService;
    private final RestClient restClient = RestClient.create();
    private final SecureRandom random = new SecureRandom();

    public OAuthService(
            OAuthProperties properties,
            OAuthSessionRepository sessions,
            OAuthIdentityRepository identities,
            UsuarioRepository users,
            JwtService jwtService) {
        this.properties = properties;
        this.sessions = sessions;
        this.identities = identities;
        this.users = users;
        this.jwtService = jwtService;
    }

    /** Inicia OAuth y devuelve un error legible al frontend si falta configuración. */
    public String beginAuthorization(String providerName) {
        String normalizedProvider = normalizeProvider(providerName);
        try {
            return authorizationUrl(normalizedProvider);
        } catch (ResponseStatusException ex) {
            log.warn("No se pudo iniciar OAuth con {}: {}", normalizedProvider, ex.getReason());
            return errorRedirect(ex.getReason() == null ? "No fue posible iniciar OAuth." : ex.getReason());
        } catch (Exception ex) {
            log.error("Error inesperado iniciando OAuth con {}", normalizedProvider, ex);
            return errorRedirect("No fue posible iniciar sesión con " + displayName(normalizedProvider) + ".");
        }
    }

    /** Construye la URL del proveedor y guarda state + code_verifier para el callback. */
    public String authorizationUrl(String providerName) {
        String normalizedProvider = normalizeProvider(providerName);
        OAuthProperties.Provider provider = provider(normalizedProvider);
        validateCredentials(normalizedProvider, provider);

        String state = UUID.randomUUID().toString();
        String verifier = randomVerifier();

        sessions.save(new OAuthSession(
                state,
                normalizedProvider,
                verifier,
                Instant.now().plus(10, ChronoUnit.MINUTES)));

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(provider.getAuthorizationUrl())
                .queryParam("client_id", provider.getClientId())
                .queryParam("redirect_uri", callbackUrl(normalizedProvider))
                .queryParam("response_type", "code")
                .queryParam("scope", provider.getScope())
                .queryParam("state", state);

        if (provider.isPkce()) {
            builder.queryParam("code_challenge", challenge(verifier));
            builder.queryParam("code_challenge_method", "S256");
        }

        // Ajustes propios de cada proveedor.
        if (normalizedProvider.equals("google")) {
            builder.queryParam("access_type", "offline");
            builder.queryParam("prompt", "select_account");
        }

        if (normalizedProvider.equals("microsoft")) {
            builder.queryParam("prompt", "select_account");
        }

        if (normalizedProvider.equals("facebook")) {
            // Meta vuelve a solicitar permisos que el usuario haya rechazado antes.
            builder.queryParam("auth_type", "rerequest");
            builder.queryParam("return_scopes", "true");
        }

        return builder.build().encode().toUriString();
    }

    /**
     * Finaliza el callback y siempre vuelve al frontend.
     * Los detalles técnicos completos quedan en el log del microservicio.
     */
    public String finishCallback(
            String providerName,
            String code,
            String state,
            String providerError,
            String providerErrorDescription) {

        String normalizedProvider = normalizeProvider(providerName);

        if (providerError != null && !providerError.isBlank()) {
            String detail = providerErrorDescription == null || providerErrorDescription.isBlank()
                    ? providerError
                    : providerErrorDescription;
            return errorRedirect(
                    "El inicio de sesión con " + displayName(normalizedProvider)
                            + " fue cancelado o rechazado: " + detail);
        }

        if (code == null || code.isBlank() || state == null || state.isBlank()) {
            return errorRedirect("El proveedor no devolvió los parámetros OAuth esperados.");
        }

        try {
            return callback(normalizedProvider, code, state);
        } catch (ResponseStatusException ex) {
            log.warn("OAuth {} rechazado por Pedidos360: {}", normalizedProvider, ex.getReason());
            return errorRedirect(ex.getReason() == null ? "No fue posible completar OAuth." : ex.getReason());
        } catch (Exception ex) {
            // Importante para diagnosticar errores de persistencia/vinculación sin mostrar secretos al navegador.
            log.error("Error inesperado completando OAuth con {}", normalizedProvider, ex);
            return errorRedirect(
                    "No fue posible completar el inicio de sesión con " + displayName(normalizedProvider)
                            + ". Revisa el log de msvc-usuario para el detalle.");
        }
    }

    /** Valida state, canjea el code, obtiene el perfil y sincroniza la identidad externa. */
    private String callback(String providerName, String code, String state) {
        OAuthProperties.Provider provider = provider(providerName);
        validateCredentials(providerName, provider);

        OAuthSession session = sessions.findById(state)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "State OAuth inválido o ya utilizado. Inicia el flujo nuevamente."));

        // Un state OAuth se consume una sola vez, incluso si el proveedor devuelve un error posterior.
        sessions.delete(session);

        if (!session.getProvider().equals(providerName) || session.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "La sesión OAuth expiró. Intenta iniciar sesión nuevamente.");
        }

        Map<String, Object> token = exchangeCode(providerName, provider, code, session.getCodeVerifier());
        String accessToken = Objects.toString(token.get("access_token"), "");
        if (accessToken.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "El proveedor no devolvió un access_token válido.");
        }

        Map<?, ?> profile = loadProfile(providerName, provider, accessToken);
        Usuario user = upsert(providerName, profile);
        String internalJwt = jwtService.create(user, providerName);

        // El JWT interno se devuelve en fragmento para no enviarlo al servidor Angular en la URL HTTP.
        return properties.getFrontendUrl() + "/auth/callback#token=" + internalJwt;
    }

    /** Canjea el authorization code por el token externo. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> exchangeCode(
            String providerName,
            OAuthProperties.Provider provider,
            String code,
            String verifier) {

        try {
            if (providerName.equals("facebook")) {
                // Meta documenta el canje mediante /oauth/access_token.
                // Si enviamos code_challenge al autorizar, se envía aquí exactamente el mismo code_verifier.
                UriComponentsBuilder tokenBuilder = UriComponentsBuilder
                        .fromUriString(provider.getTokenUrl())
                        .queryParam("client_id", provider.getClientId())
                        .queryParam("client_secret", provider.getClientSecret())
                        .queryParam("redirect_uri", callbackUrl(providerName))
                        .queryParam("code", code);

                if (provider.isPkce()) {
                    if (verifier == null || verifier.isBlank()) {
                        throw new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "La sesión PKCE de Facebook no contiene code_verifier. Inicia sesión nuevamente.");
                    }
                    tokenBuilder.queryParam("code_verifier", verifier);
                }

                URI tokenUri = tokenBuilder.build().encode().toUri();
                Map<String, Object> body = restClient.get()
                        .uri(tokenUri)
                        .retrieve()
                        .body(Map.class);

                return body == null ? Map.of() : body;
            }

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "authorization_code");
            form.add("client_id", provider.getClientId());
            form.add("client_secret", provider.getClientSecret());
            form.add("code", code);
            form.add("redirect_uri", callbackUrl(providerName));

            if (provider.isPkce()) {
                if (verifier == null || verifier.isBlank()) {
                    throw new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED,
                            "La sesión PKCE no contiene code_verifier. Inicia sesión nuevamente.");
                }
                form.add("code_verifier", verifier);
            }

            Map<String, Object> body = restClient.post()
                    .uri(provider.getTokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);

            return body == null ? Map.of() : body;
        } catch (RestClientResponseException ex) {
            log.warn(
                    "OAuth token exchange failed for {} with status {}: {}",
                    providerName,
                    ex.getStatusCode(),
                    safeProviderMessage(ex));

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No se pudo canjear el código de " + displayName(providerName)
                            + ". Verifica Client ID, Client Secret y que la Redirect URI sea exactamente "
                            + callbackUrl(providerName) + ".",
                    ex);
        }
    }

    /** Obtiene el perfil básico usando el access token del proveedor. */
    private Map<?, ?> loadProfile(
            String providerName,
            OAuthProperties.Provider provider,
            String accessToken) {

        try {
            if (providerName.equals("facebook")) {
                // appsecret_proof hace compatible la llamada con aplicaciones Meta
                // que tienen habilitado "Require App Secret" para llamadas del servidor.
                URI profileUri = UriComponentsBuilder
                        .fromUriString(provider.getUserInfoUrl())
                        .queryParam("access_token", accessToken)
                        .queryParam("appsecret_proof", facebookAppSecretProof(provider.getClientSecret(), accessToken))
                        .build()
                        .encode()
                        .toUri();

                Map<?, ?> body = restClient.get()
                        .uri(profileUri)
                        .retrieve()
                        .body(Map.class);

                return body == null ? Map.of() : body;
            }

            Map<?, ?> body = restClient.get()
                    .uri(provider.getUserInfoUrl())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            return body == null ? Map.of() : body;
        } catch (RestClientResponseException ex) {
            log.warn(
                    "OAuth profile request failed for {} with status {}: {}",
                    providerName,
                    ex.getStatusCode(),
                    safeProviderMessage(ex));

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No se pudo leer el perfil de " + displayName(providerName)
                            + ". Revisa que el permiso de correo esté concedido.",
                    ex);
        }
    }

    /**
     * Crea o vincula una identidad externa sin sobrescribir vínculos anteriores.
     * Esto permite que un mismo correo use Google, Facebook, Discord y Microsoft.
     */
    private Usuario upsert(String provider, Map<?, ?> profile) {
        String providerId = first(profile, "sub", "id");
        String rawEmail = first(profile, "email", "preferred_username", "upn");
        String fullName = first(profile, "name", "global_name", "username");

        if (providerId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "El proveedor no entregó un identificador de usuario.");
        }

        boolean providerReturnedEmail = rawEmail != null && !rawEmail.isBlank();
        String normalizedEmail = providerReturnedEmail
                ? rawEmail.trim().toLowerCase(Locale.ROOT)
                : syntheticEmail(provider, providerId);

        Optional<OAuthIdentity> identityMatch = identities
                .findByProviderAndProviderUserId(provider, providerId);

        Usuario identityUser = identityMatch
                .flatMap(identity -> users.findById(identity.getUsuarioId()))
                .orElse(null);

        // Compatibilidad con usuarios creados por versiones anteriores a oauth_identities.
        Usuario legacyUser = users.findByProviderAndProviderUserId(provider, providerId).orElse(null);
        Usuario emailUser = providerReturnedEmail
                ? users.findByEmailIgnoreCase(normalizedEmail).orElse(null)
                : null;

        Usuario user = chooseUserForIdentity(provider, identityUser, legacyUser, emailUser);
        if (user == null) {
            user = new Usuario();
            user.setEmail(normalizedEmail);
            user.setProvider(provider);
        }

        // Si una identidad antigua quedó con correo sintético y ahora Facebook entrega
        // el correo real, reutilizamos la cuenta real existente en lugar de chocar con UNIQUE(email).
        if (providerReturnedEmail
                && emailUser != null
                && user.getId() != null
                && !emailUser.getId().equals(user.getId())
                && isSyntheticOAuthEmail(user.getEmail())) {
            user = emailUser;
        }

        String[] parts = splitName(fullName, normalizedEmail);
        user.setNombre(parts[0]);
        user.setApellido(parts[1]);

        // Solo reemplazamos el correo si no provoca una colisión con otra cuenta.
        if (providerReturnedEmail) {
            Optional<Usuario> owner = users.findByEmailIgnoreCase(normalizedEmail);
            if (owner.isEmpty() || user.getId() == null || owner.get().getId().equals(user.getId())) {
                user.setEmail(normalizedEmail);
            }
        } else if (user.getEmail() == null || user.getEmail().isBlank()) {
            user.setEmail(normalizedEmail);
        }

        // Campos legacy: se conservan para compatibilidad y para mostrar el último proveedor usado.
        user.setProvider(provider);
        user.setProviderUserId(providerId);

        try {
            user = users.save(user);
            linkIdentity(user, provider, providerId, identityMatch.orElse(null));
            return user;
        } catch (DataIntegrityViolationException ex) {
            log.error("Conflicto persistiendo identidad OAuth {} / {}", provider, providerId, ex);
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La cuenta externa se autenticó correctamente, pero el correo ya está asociado a otra cuenta de Pedidos360. "
                            + "Inicia sesión con ese correo y vuelve a vincular el proveedor.",
                    ex);
        }
    }

    /** Decide qué usuario local debe recibir la identidad externa. */
    private Usuario chooseUserForIdentity(
            String provider,
            Usuario identityUser,
            Usuario legacyUser,
            Usuario emailUser) {

        if (identityUser != null) {
            if (emailUser != null
                    && !emailUser.getId().equals(identityUser.getId())
                    && isSyntheticOAuthEmail(identityUser.getEmail())) {
                return emailUser;
            }
            return identityUser;
        }

        if (legacyUser != null) {
            if (emailUser != null
                    && !emailUser.getId().equals(legacyUser.getId())
                    && isSyntheticOAuthEmail(legacyUser.getEmail())) {
                return emailUser;
            }
            return legacyUser;
        }

        return emailUser;
    }

    /** Crea o reubica el vínculo proveedor -> usuario local. */
    private void linkIdentity(
            Usuario user,
            String provider,
            String providerId,
            OAuthIdentity existingIdentity) {

        OAuthIdentity identity = existingIdentity;

        if (identity == null) {
            identity = identities.findByUsuarioIdAndProvider(user.getId(), provider)
                    .orElseGet(() -> new OAuthIdentity(user.getId(), provider, providerId));
        }

        identity.setUsuarioId(user.getId());
        identity.setProvider(provider);
        identity.setProviderUserId(providerId);
        identities.save(identity);
    }

    /** Evita iniciar un flujo OAuth incompleto que terminaría en un error difícil de entender. */
    private void validateCredentials(String providerName, OAuthProperties.Provider provider) {
        if (provider.getClientId() == null || provider.getClientId().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Falta configurar el Client ID de " + displayName(providerName) + ".");
        }

        if (provider.getClientSecret() == null || provider.getClientSecret().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Falta configurar el Client Secret de " + displayName(providerName)
                            + " en pedidos360-backend/.env.");
        }

        if (provider.getAuthorizationUrl() == null || provider.getAuthorizationUrl().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Falta configurar la URL de autorización de " + displayName(providerName) + ".");
        }

        if (provider.getTokenUrl() == null || provider.getTokenUrl().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Falta configurar la URL de token de " + displayName(providerName) + ".");
        }

        // En el tenant External ID de Pedidos360 los endpoints CIAM deben pertenecer
        // al mismo tenant configurado en MICROSOFT_TENANT_ID.
        if (providerName.equals("microsoft")) {
            String tenantId = provider.getTenantId();
            if (tenantId == null || tenantId.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Falta configurar MICROSOFT_TENANT_ID en pedidos360-backend/.env.");
            }

            boolean authMatchesTenant = provider.getAuthorizationUrl().contains(tenantId);
            boolean tokenMatchesTenant = provider.getTokenUrl().contains(tenantId);
            if (!authMatchesTenant || !tokenMatchesTenant) {
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "La configuración de Microsoft mezcla tenants. MICROSOFT_AUTH_URL y MICROSOFT_TOKEN_URL deben corresponder a MICROSOFT_TENANT_ID.");
            }
        }
    }

    private OAuthProperties.Provider provider(String name) {
        OAuthProperties.Provider provider = properties.getProviders().get(normalizeProvider(name));
        if (provider == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Proveedor OAuth no soportado.");
        }
        return provider;
    }

    private String callbackUrl(String provider) {
        return properties.getGatewayPublicUrl() + "/api/auth/oauth2/callback/" + normalizeProvider(provider);
    }

    private String errorRedirect(String message) {
        return UriComponentsBuilder
                .fromUriString(properties.getFrontendUrl())
                .queryParam("oauth_error", message)
                .build()
                .encode()
                .toUriString();
    }

    private String randomVerifier() {
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String challenge(String verifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar el code_challenge PKCE.", ex);
        }
    }

    /** Meta recomienda appsecret_proof para llamadas servidor-servidor cuando está exigido en la app. */
    private String facebookAppSecretProof(String clientSecret, String accessToken) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(clientSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(hmac.doFinal(accessToken.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar appsecret_proof para Facebook.", ex);
        }
    }

    private String[] splitName(String fullName, String fallbackEmail) {
        if (fullName == null || fullName.isBlank()) {
            String fallback = fallbackEmail == null || fallbackEmail.isBlank()
                    ? "Usuario"
                    : fallbackEmail.split("@")[0];
            return new String[]{fallback, ""};
        }

        String[] parts = fullName.trim().split("\\s+", 2);
        return new String[]{parts[0], parts.length > 1 ? parts[1] : ""};
    }

    private String syntheticEmail(String provider, String providerId) {
        return provider.toLowerCase(Locale.ROOT) + "." + providerId + "@oauth.pedidos360.local";
    }

    private boolean isSyntheticOAuthEmail(String email) {
        return email != null && email.toLowerCase(Locale.ROOT).endsWith("@oauth.pedidos360.local");
    }

    private String first(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value instanceof Collection<?> collection && !collection.isEmpty()) {
                return String.valueOf(collection.iterator().next());
            }
            if (value != null && !String.valueOf(value).isBlank()) {
                return String.valueOf(value);
            }
        }
        return "";
    }

    /** Recorta la respuesta del proveedor para dejar diagnóstico útil sin imprimir tokens. */
    private String safeProviderMessage(RestClientResponseException ex) {
        String body = ex.getResponseBodyAsString();
        if (body == null || body.isBlank()) return "sin detalle";

        String sanitized = body
                .replaceAll("(?i)\"access_token\"\\s*:\\s*\"[^\"]+\"", "\"access_token\":\"[REDACTED]\"")
                .replaceAll("(?i)client_secret=[^&\\s]+", "client_secret=[REDACTED]");

        return sanitized.length() > 500
                ? sanitized.substring(0, 500) + "…"
                : sanitized;
    }

    private String normalizeProvider(String provider) {
        return provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT);
    }

    private String displayName(String provider) {
        return switch (normalizeProvider(provider)) {
            case "google" -> "Google";
            case "facebook" -> "Facebook";
            case "discord" -> "Discord";
            case "microsoft" -> "Microsoft";
            default -> provider;
        };
    }
}

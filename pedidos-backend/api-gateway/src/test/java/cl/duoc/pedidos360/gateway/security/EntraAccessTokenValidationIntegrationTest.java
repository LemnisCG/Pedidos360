package cl.duoc.pedidos360.gateway.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Levanta el gateway completo contra un servidor local que simula dos cosas:
 * a Entra External ID (documento OpenID y claves públicas JWKS) y a los microservicios.
 * Así se prueba la validación real de firma, emisor, audiencia, vigencia, scope y rol
 * sin depender de Internet ni de una cuenta del tenant.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EntraAccessTokenValidationIntegrationTest {

    private static final String API_AUDIENCE_CLIENT_ID = "client-id-de-la-api-en-pruebas";
    private static final String REQUIRED_SCOPE = "BFF.Access";
    private static final String AUTHENTICATED_USER_OBJECT_ID = "9f1c2d3e-4b5a-4c6d-8e7f-0a1b2c3d4e5f";
    private static final String SPOOFED_USER_OBJECT_ID = "00000000-0000-0000-0000-000000000000";

    private static final HttpServer fakeEntraAndMicroservicesServer;
    private static final RSAKey signingKeyPublishedByFakeEntra;
    private static final RSAKey signingKeyUnknownToFakeEntra;
    private static final String fakeEntraIssuerUri;
    private static final String fakeServerBaseUrl;

    static {
        try {
            signingKeyPublishedByFakeEntra = new RSAKeyGenerator(2048).keyID("clave-publicada").generate();
            signingKeyUnknownToFakeEntra = new RSAKeyGenerator(2048).keyID("clave-desconocida").generate();

            fakeEntraAndMicroservicesServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            fakeServerBaseUrl = "http://localhost:" + fakeEntraAndMicroservicesServer.getAddress().getPort();
            fakeEntraIssuerUri = fakeServerBaseUrl + "/tenant-de-pruebas/v2.0";

            String openIdConfigurationJson = """
                    {"issuer":"%s","jwks_uri":"%s/tenant-de-pruebas/keys",
                     "subject_types_supported":["pairwise"],"id_token_signing_alg_values_supported":["RS256"]}
                    """.formatted(fakeEntraIssuerUri, fakeServerBaseUrl);
            String publishedJwksJson = new JWKSet(signingKeyPublishedByFakeEntra.toPublicJWK()).toString();

            fakeEntraAndMicroservicesServer.createContext("/tenant-de-pruebas/v2.0/.well-known/openid-configuration",
                    exchange -> respondWithJson(exchange, openIdConfigurationJson));
            fakeEntraAndMicroservicesServer.createContext("/tenant-de-pruebas/keys",
                    exchange -> respondWithJson(exchange, publishedJwksJson));
            // Microservicio simulado: devuelve la identidad que recibió del gateway.
            fakeEntraAndMicroservicesServer.createContext("/api/", exchange -> respondWithJson(exchange,
                    "{\"userIdReceivedByMicroservice\":\"%s\",\"emailReceivedByMicroservice\":\"%s\"}".formatted(
                            exchange.getRequestHeaders().getFirst("X-User-Id"),
                            exchange.getRequestHeaders().getFirst("X-User-Email"))));
            fakeEntraAndMicroservicesServer.start();
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo iniciar el servidor simulado de pruebas", exception);
        }
    }

    @DynamicPropertySource
    static void pointGatewayToFakeEntraAndMicroservices(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> fakeEntraIssuerUri);
        registry.add("spring.security.oauth2.resourceserver.jwt.audiences", () -> API_AUDIENCE_CLIENT_ID);
        registry.add("pedidos360.security.required-access-token-scope", () -> REQUIRED_SCOPE);
        for (String microservice : List.of("usuario", "producto", "carrito", "catalogo", "pago", "envio")) {
            registry.add("pedidos360.services." + microservice, () -> fakeServerBaseUrl);
        }
    }

    @Value("${local.server.port}")
    private int gatewayPort;

    private WebTestClient gatewayClient;

    @BeforeEach
    void createGatewayClient() {
        gatewayClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + gatewayPort).build();
    }

    @Test
    void publicCatalogIsReachableWithoutAccessToken() {
        gatewayClient.get().uri("/api/catalogo").exchange().expectStatus().isOk();
    }

    @Test
    void protectedRouteWithoutAccessTokenRespondsUnauthorizedWithJsonError() {
        gatewayClient.get().uri("/api/carrito").exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueEquals("WWW-Authenticate", "Bearer")
                .expectBody().jsonPath("$.error").isNotEmpty();
    }

    @Test
    void accessTokenSignedWithKeyNotPublishedByEntraRespondsUnauthorized() {
        String accessToken = createAccessToken(signingKeyUnknownToFakeEntra, claims -> { });
        gatewayClient.get().uri("/api/carrito").headers(headers -> headers.setBearerAuth(accessToken)).exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void expiredAccessTokenRespondsUnauthorized() {
        Instant tenMinutesAgo = Instant.now().minus(10, ChronoUnit.MINUTES);
        String accessToken = createAccessToken(signingKeyPublishedByFakeEntra,
                claims -> claims.expirationTime(Date.from(tenMinutesAgo)));
        gatewayClient.get().uri("/api/carrito").headers(headers -> headers.setBearerAuth(accessToken)).exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void accessTokenIssuedForAnotherAudienceRespondsUnauthorized() {
        String accessToken = createAccessToken(signingKeyPublishedByFakeEntra,
                claims -> claims.audience("client-id-de-otra-aplicacion"));
        gatewayClient.get().uri("/api/carrito").headers(headers -> headers.setBearerAuth(accessToken)).exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void accessTokenFromAnotherIssuerRespondsUnauthorized() {
        String accessToken = createAccessToken(signingKeyPublishedByFakeEntra,
                claims -> claims.issuer("https://otro-tenant.ciamlogin.com/otro-tenant/v2.0"));
        gatewayClient.get().uri("/api/carrito").headers(headers -> headers.setBearerAuth(accessToken)).exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void validAccessTokenWithoutRequiredScopeRespondsForbidden() {
        String accessToken = createAccessToken(signingKeyPublishedByFakeEntra,
                claims -> claims.claim("scp", "Otro.Scope"));
        gatewayClient.get().uri("/api/carrito").headers(headers -> headers.setBearerAuth(accessToken)).exchange()
                .expectStatus().isForbidden()
                .expectBody().jsonPath("$.error").isNotEmpty();
    }

    @Test
    void validAccessTokenReachesMicroserviceWithIdentityTakenFromTokenAndNotFromClientHeaders() {
        String accessToken = createAccessToken(signingKeyPublishedByFakeEntra, claims -> { });
        gatewayClient.get().uri("/api/carrito")
                .headers(headers -> headers.setBearerAuth(accessToken))
                .header("X-User-Id", SPOOFED_USER_OBJECT_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.userIdReceivedByMicroservice").isEqualTo(AUTHENTICATED_USER_OBJECT_ID)
                .jsonPath("$.emailReceivedByMicroservice").isEqualTo("cliente@pedidos360.test");
    }

    @Test
    void clientWithoutAdministradorRoleCannotUpdateCatalogBanner() {
        String accessTokenOfSelfRegisteredClient = createAccessToken(signingKeyPublishedByFakeEntra, claims -> { });
        gatewayClient.put().uri("/api/catalogo/banner")
                .headers(headers -> headers.setBearerAuth(accessTokenOfSelfRegisteredClient))
                .contentType(MediaType.APPLICATION_JSON).bodyValue("{}")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void userWithAdministradorRoleCanUpdateCatalogBanner() {
        String accessTokenOfAdministrador = createAccessToken(signingKeyPublishedByFakeEntra,
                claims -> claims.claim("roles", List.of(EntraAccessTokenSecurityConfiguration.ADMINISTRADOR_APP_ROLE)));
        gatewayClient.put().uri("/api/catalogo/banner")
                .headers(headers -> headers.setBearerAuth(accessTokenOfAdministrador))
                .contentType(MediaType.APPLICATION_JSON).bodyValue("{}")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void forgedIdentityHeaderOnPublicRouteIsNotForwardedToMicroservice() {
        gatewayClient.get().uri("/api/catalogo").header("X-User-Id", SPOOFED_USER_OBJECT_ID).exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(body -> assertThat(body).doesNotContain(SPOOFED_USER_OBJECT_ID));
    }

    /** Token válido de un cliente auto-registrado (sin roles); cada prueba altera solo el claim que evalúa. */
    private static String createAccessToken(RSAKey signingKey, Consumer<JWTClaimsSet.Builder> customizeClaims) {
        try {
            Instant now = Instant.now();
            JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                    .issuer(fakeEntraIssuerUri)
                    .audience(API_AUDIENCE_CLIENT_ID)
                    .subject("sub-pairwise-de-pruebas")
                    .issueTime(Date.from(now.minus(20, ChronoUnit.MINUTES)))
                    .expirationTime(Date.from(now.plus(10, ChronoUnit.MINUTES)))
                    .claim("oid", AUTHENTICATED_USER_OBJECT_ID)
                    .claim("scp", REQUIRED_SCOPE)
                    .claim("email", "cliente@pedidos360.test")
                    .claim("name", "Cliente De Pruebas");
            customizeClaims.accept(claims);

            SignedJWT signedAccessToken = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingKey.getKeyID()).build(), claims.build());
            signedAccessToken.sign(new RSASSASigner(signingKey));
            return signedAccessToken.serialize();
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo firmar el access token de pruebas", exception);
        }
    }

    private static void respondWithJson(HttpExchange exchange, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getRequestBody().readAllBytes();
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }
}

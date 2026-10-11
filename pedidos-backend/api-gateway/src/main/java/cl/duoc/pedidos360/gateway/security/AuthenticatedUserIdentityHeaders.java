package cl.duoc.pedidos360.gateway.security;

import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Locale;

/**
 * Cabeceras internas con las que el gateway informa a los microservicios quién es el usuario.
 * Solo el gateway puede escribirlas: las que envíe el navegador se descartan para que nadie
 * pueda suplantar a otro usuario.
 */
public final class AuthenticatedUserIdentityHeaders {

    public static final String ENTRA_USER_OBJECT_ID_CLAIM = "oid";

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_EMAIL_HEADER = "X-User-Email";
    private static final String USER_NAME_HEADER = "X-User-Name";
    private static final String[] RESERVED_HEADER_PREFIXES = {"x-user-", "x-auth-"};

    private AuthenticatedUserIdentityHeaders() {
    }

    public static boolean isReservedForGateway(String headerName) {
        String normalizedHeaderName = headerName.toLowerCase(Locale.ROOT);
        for (String reservedPrefix : RESERVED_HEADER_PREFIXES) {
            if (normalizedHeaderName.startsWith(reservedPrefix)) return true;
        }
        return false;
    }

    /** Copia la identidad desde los claims del access token ya validado. */
    public static void writeFromValidatedAccessToken(HttpHeaders downstreamHeaders, Jwt validatedAccessToken) {
        String userObjectId = validatedAccessToken.getClaimAsString(ENTRA_USER_OBJECT_ID_CLAIM);
        String email = firstNonBlank(
                validatedAccessToken.getClaimAsString("email"),
                validatedAccessToken.getClaimAsString("preferred_username"));
        String name = firstNonBlank(validatedAccessToken.getClaimAsString("name"), email);

        if (userObjectId != null) downstreamHeaders.set(USER_ID_HEADER, userObjectId);
        downstreamHeaders.set(USER_EMAIL_HEADER, email);
        downstreamHeaders.set(USER_NAME_HEADER, name);
    }

    private static String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) return candidate;
        }
        return "";
    }
}

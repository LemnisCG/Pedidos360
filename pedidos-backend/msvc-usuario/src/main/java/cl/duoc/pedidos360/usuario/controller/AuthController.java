package cl.duoc.pedidos360.usuario.controller;

import cl.duoc.pedidos360.usuario.dto.AuthDtos.*;
import cl.duoc.pedidos360.usuario.oauth.OAuthService;
import cl.duoc.pedidos360.usuario.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * Endpoints de autenticación local y federada.
 * Los errores OAuth vuelven al frontend para evitar la página Whitelabel de Spring.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final OAuthService oauthService;

    public AuthController(AuthService authService, OAuthService oauthService) {
        this.authService = authService;
        this.oauthService = oauthService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/oauth2/{provider}/authorize")
    public ResponseEntity<Void> authorize(@PathVariable String provider) {
        return ResponseEntity.status(302)
                .header(HttpHeaders.LOCATION, oauthService.beginAuthorization(provider))
                .build();
    }

    @GetMapping("/oauth2/callback/{provider}")
    public ResponseEntity<Void> callback(
            @PathVariable String provider,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription) {

        String target = oauthService.finishCallback(provider, code, state, error, errorDescription);
        return ResponseEntity.status(302).location(URI.create(target)).build();
    }
}

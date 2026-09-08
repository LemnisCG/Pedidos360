package com.pedidos360.bff.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class TestController {

    // Esta ruta está protegida automáticamente por tu SecurityFilterChain
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testToken(@AuthenticationPrincipal Jwt jwt) {
        // @AuthenticationPrincipal Jwt jwt te permite leer los datos que vienen DENTRO del token de Azure
        
        return ResponseEntity.ok(Map.of(
            "mensaje", "¡Token validado con éxito por Spring Boot!",
            "usuario_azure", jwt.getClaimAsString("name"), // Nombre del usuario
            "correo", jwt.getClaimAsString("preferred_username"), // Su email
            "roles_o_scopes", jwt.getClaims() // Todo lo que Azure metió en el token
        ));
    }
}

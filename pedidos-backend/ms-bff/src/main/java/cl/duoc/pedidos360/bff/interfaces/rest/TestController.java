package cl.duoc.pedidos360.bff.controllers;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/secure")
    public Map<String, Object> testSecurity(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> response = new HashMap<>();
        
        // Extraer datos específicos del token de Azure
        response.put("mensaje", "¡Petición segura al BFF exitosa!");
        response.put("usuario_oid", jwt.getClaimAsString("oid"));
        
        // Imprimir todo el payload para inspeccionarlo en la respuesta
        response.put("token_claims", jwt.getClaims());
        
        return response;
    }
}
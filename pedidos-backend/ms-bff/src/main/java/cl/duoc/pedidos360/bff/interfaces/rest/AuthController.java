package cl.duoc.pedidos360.bff.controllers;


import cl.duoc.pedidos360.bff.application.PerfilClientService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final PerfilClientService perfilClientService;

    // Inyección de dependencias (equivalente al constructor en NestJS)
    public AuthController(PerfilClientService perfilClientService) {
        this.perfilClientService = perfilClientService;
    }

    @GetMapping("/login")
    public Map<String, Object> procesarLogin(@AuthenticationPrincipal Jwt jwt) {
        // 1. Extraer los claims del token de Entra External ID
        String oid = jwt.getClaimAsString("oid");
        
        // Dependiendo de tu configuración en Azure, el email puede venir en "emails" (array) o "preferred_username"
        String email = jwt.getClaimAsString("preferred_username"); 
        String nombre = jwt.getClaimAsString("name");

        // 2. Delegar la llamada HTTP a ms-perfil
        // Esto enviará el payload y devolverá la respuesta final al frontend
        return perfilClientService.sincronizarPerfil(oid, email, nombre);
    }
}
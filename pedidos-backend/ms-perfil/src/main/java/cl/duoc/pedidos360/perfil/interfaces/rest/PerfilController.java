package cl.duoc.pedidos360.perfil.interfaces.rest;

import cl.duoc.pedidos360.perfil.application.PerfilService;
import cl.duoc.pedidos360.perfil.domain.model.Perfil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/perfiles")
public class PerfilController {

    private final PerfilService perfilService;

    public PerfilController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    @GetMapping
    public List<Perfil> listar() {
        return perfilService.listar();
    }
}
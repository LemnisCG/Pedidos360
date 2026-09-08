package cl.duoc.pedidos360.perfil.interfaces.rest;

import cl.duoc.pedidos360.perfil.application.PerfilService;
import cl.duoc.pedidos360.perfil.domain.model.Perfil;
import cl.duoc.pedidos360.perfil.interfaces.rest.dto.CreatePerfilRequestDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Perfil createPerfil(@Valid @RequestBody CreatePerfilRequestDto request) {
        return perfilService.createPerfil(
                request.idProvider(),
                request.email(),
                request.nombre(),
                request.apellido(),
                request.direccionEnvio(),
                request.telefono()
        );
    }

}
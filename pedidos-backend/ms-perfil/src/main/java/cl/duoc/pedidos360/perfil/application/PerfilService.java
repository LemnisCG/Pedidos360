package cl.duoc.pedidos360.perfil.application;

import cl.duoc.pedidos360.perfil.domain.model.Perfil;
import cl.duoc.pedidos360.perfil.infrastructure.persistence.PerfilRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PerfilService {

    private final PerfilRepository perfilRepository;

    public PerfilService(PerfilRepository perfilRepository) {
        this.perfilRepository = perfilRepository;
    }

    public List<Perfil> listar() {
        return perfilRepository.findAll();
    }

    public Perfil createPerfil(UUID idProvider, String email, String nombre,
                               String apellido, String direccionEnvio, String telefono) {
        Perfil nuevoPerfil = new Perfil(
                idProvider,
                email,
                nombre,
                apellido,
                direccionEnvio,
                telefono
        );
        return perfilRepository.save(nuevoPerfil);
    }
}
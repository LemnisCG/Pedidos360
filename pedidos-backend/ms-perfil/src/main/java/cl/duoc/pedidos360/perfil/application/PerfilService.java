package cl.duoc.pedidos360.perfil.application;

import cl.duoc.pedidos360.perfil.domain.model.Perfil;
import cl.duoc.pedidos360.perfil.infrastructure.persistence.PerfilRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PerfilService {

    private final PerfilRepository perfilRepository;

    public PerfilService(PerfilRepository perfilRepository) {
        this.perfilRepository = perfilRepository;
    }

    public List<Perfil> listar() {
        return perfilRepository.findAll();
    }
}
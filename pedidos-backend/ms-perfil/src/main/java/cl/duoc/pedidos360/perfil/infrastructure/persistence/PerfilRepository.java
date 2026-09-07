package cl.duoc.pedidos360.perfil.infrastructure.persistence;

import cl.duoc.pedidos360.perfil.domain.model.Perfil;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PerfilRepository extends JpaRepository<Perfil, Long> {
}
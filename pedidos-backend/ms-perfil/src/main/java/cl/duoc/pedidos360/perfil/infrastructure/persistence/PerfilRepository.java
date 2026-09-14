package cl.duoc.pedidos360.perfil.infrastructure.persistence;

import cl.duoc.pedidos360.perfil.domain.model.Perfil;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PerfilRepository extends JpaRepository<Perfil, Long> {

    Optional<Perfil> findByIdProvider(UUID oid);
}
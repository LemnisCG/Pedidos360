package cl.duoc.pedidos360.envio.repository;

import cl.duoc.pedidos360.envio.model.Envio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Repositorio exclusivo de msvc-envio. */
public interface EnvioRepository extends JpaRepository<Envio, UUID> {
}

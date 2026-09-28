package cl.duoc.pedidos360.catalogo.repository;

import cl.duoc.pedidos360.catalogo.model.CatalogoConfig;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio de la configuración visual del catálogo. */
public interface CatalogoConfigRepository extends JpaRepository<CatalogoConfig, Long> {
}

package cl.duoc.catalogo.repository;

import cl.duoc.catalogo.model.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CatalogoRepository extends JpaRepository<Producto, Long> {
}
package cl.duoc.pedidos360.carrito.repository;

import cl.duoc.pedidos360.carrito.model.CarritoItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Repositorio de acceso a la base exclusiva de msvc-carrito. */
public interface CarritoRepository extends JpaRepository<CarritoItem, Long> {

    List<CarritoItem> findAllByUsuarioId(UUID usuarioId);

    Optional<CarritoItem> findByUsuarioIdAndProductoId(UUID usuarioId, Long productoId);

    void deleteAllByUsuarioId(UUID usuarioId);
}

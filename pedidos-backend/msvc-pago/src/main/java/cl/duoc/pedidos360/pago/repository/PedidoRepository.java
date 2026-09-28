package cl.duoc.pedidos360.pago.repository;

import cl.duoc.pedidos360.pago.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Repositorio del historial de compras perteneciente exclusivamente a msvc-pago. */
public interface PedidoRepository extends JpaRepository<Pedido, UUID> {

    List<Pedido> findAllByUsuarioIdOrderByCreadoEnDesc(UUID usuarioId);

    Optional<Pedido> findByIdAndUsuarioId(UUID id, UUID usuarioId);
}

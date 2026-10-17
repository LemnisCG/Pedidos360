package cl.duoc.pedidos360.envio.repository;

import cl.duoc.pedidos360.envio.model.EventoAuditoriaPedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Repositorio del historial de auditoría de los pedidos. */
public interface EventoAuditoriaPedidoRepository extends JpaRepository<EventoAuditoriaPedido, UUID> {

    boolean existsByPedidoIdAndTipoEvento(UUID pedidoId, String tipoEvento);
}

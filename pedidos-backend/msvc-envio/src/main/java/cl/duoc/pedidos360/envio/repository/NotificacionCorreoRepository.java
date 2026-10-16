package cl.duoc.pedidos360.envio.repository;

import cl.duoc.pedidos360.envio.model.NotificacionCorreo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Repositorio de las notificaciones por correo de msvc-envio. */
public interface NotificacionCorreoRepository extends JpaRepository<NotificacionCorreo, UUID> {

    boolean existsByPedidoId(UUID pedidoId);
}

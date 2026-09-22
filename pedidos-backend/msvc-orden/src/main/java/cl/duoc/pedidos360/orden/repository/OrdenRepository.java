package cl.duoc.pedidos360.orden.repository;

import cl.duoc.pedidos360.orden.model.Orden;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface OrdenRepository extends JpaRepository<Orden, UUID> {
}
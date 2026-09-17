package cl.duoc.pedidos360.pago.repository;

import cl.duoc.pedidos360.pago.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface PagoRepository extends JpaRepository<Pago, UUID> {
}
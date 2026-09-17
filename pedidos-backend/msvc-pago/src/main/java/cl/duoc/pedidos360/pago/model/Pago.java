package cl.duoc.pedidos360.pago.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pago {
    @Id
    private UUID id;
    private BigDecimal monto;
    private String moneda;
    private String metodo;
    private String ultimos4;
    private String estado;
    private Instant creadoEn;
}
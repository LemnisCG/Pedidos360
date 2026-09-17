package cl.duoc.pedidos360.orden.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ordenes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Orden {
    @Id
    private UUID id;

    private String numeroOrden;
    private Instant fecha;
    private String estado;
    private String metodoPago;
    private BigDecimal total;

    // Datos del usuario autenticado y de entrega del pedido.
    private String clienteEmail;

    @Column(length = 600)
    private String direccionEnvio;

    private Boolean confirmacionEmailEnviada;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "orden_items", joinColumns = @JoinColumn(name = "orden_id"))
    private List<OrdenItem> items;
}

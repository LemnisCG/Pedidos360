package cl.duoc.pedidos360.orden.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdenItem {
    private Long productoId;
    private String nombre;
    private BigDecimal precioUnitario;
    private Integer cantidad;
    private String codigoJuego;
    private String imagenUrl;
}
package cl.duoc.pedidos360.producto.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "productos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Producto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String nombre;
    @Column(length = 1200)
    private String descripcion;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;
    private Integer stock;
    private String imagenUrl;
    private String categoria;
    private LocalDate fechaLanzamiento;
}

package cl.duoc.pedidos360.producto.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Solicitud interna de descuento de inventario. */
public record StockRequest(
        @NotNull Long productoId,
        @Min(1) int cantidad
) {
}

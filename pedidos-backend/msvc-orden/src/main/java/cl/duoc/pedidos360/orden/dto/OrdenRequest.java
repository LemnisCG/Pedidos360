package cl.duoc.pedidos360.orden.dto;

import java.math.BigDecimal;
import java.util.List;

/*
 * Datos enviados por Angular una vez que msvc-pago aprueba el pago demo.
 * El correo del cliente proviene de la cuenta autenticada con Microsoft Entra.
 */
public record OrdenRequest(
        String paymentId,
        String clienteEmail,
        String direccionEnvio,
        List<Item> items
) {
    public record Item(
            Long productoId,
            String nombre,
            BigDecimal precioUnitario,
            Integer cantidad,
            String imagenUrl
    ) {}
}

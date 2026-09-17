package cl.duoc.pedidos360.pago.dto;

import java.math.BigDecimal;

public record PagoRequest(BigDecimal monto, String moneda, String metodo, String ultimos4) {
}
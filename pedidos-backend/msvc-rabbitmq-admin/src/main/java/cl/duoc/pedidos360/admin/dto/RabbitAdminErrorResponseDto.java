package cl.duoc.pedidos360.admin.dto;

import java.util.List;

/** Respuesta de error de la API: un resumen y el detalle de cada problema encontrado. */
public record RabbitAdminErrorResponseDto(String error, List<String> details) {
}

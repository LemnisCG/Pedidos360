package cl.duoc.pedidos360.admin.controller;

import cl.duoc.pedidos360.admin.dto.RabbitAdminErrorResponseDto;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpException;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.List;

/** Traduce los errores de validación y de RabbitMQ a respuestas JSON con mensajes claros. */
@RestControllerAdvice
public class RabbitAdminApiExceptionHandler {

    private static final String INVALID_INPUT = "Datos de entrada inválidos";

    /** Cuerpo JSON que no cumple las validaciones del DTO. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RabbitAdminErrorResponseDto> handleInvalidBody(MethodArgumentNotValidException e) {
        List<String> details = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .toList();
        return badRequest(details);
    }

    /** Parámetros de ruta o de consulta que no cumplen las validaciones. */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<RabbitAdminErrorResponseDto> handleInvalidParameters(HandlerMethodValidationException e) {
        List<String> details = e.getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .toList();
        return badRequest(details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RabbitAdminErrorResponseDto> handleUnreadableBody(HttpMessageNotReadableException e) {
        return badRequest(List.of("El cuerpo de la petición debe ser un JSON válido"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<RabbitAdminErrorResponseDto> handleMissingParameter(MissingServletRequestParameterException e) {
        return badRequest(List.of("Falta el parámetro obligatorio: " + e.getParameterName()));
    }

    @ExceptionHandler(AmqpConnectException.class)
    public ResponseEntity<RabbitAdminErrorResponseDto> handleBrokerUnavailable(AmqpConnectException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new RabbitAdminErrorResponseDto("RabbitMQ no está disponible", List.of()));
    }

    /** RabbitMQ rechazó la operación, por ejemplo un binding hacia una cola que no existe. */
    @ExceptionHandler(AmqpException.class)
    public ResponseEntity<RabbitAdminErrorResponseDto> handleBrokerRejection(AmqpException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new RabbitAdminErrorResponseDto(
                        "RabbitMQ rechazó la operación",
                        List.of(NestedExceptionUtils.getMostSpecificCause(e).getMessage())));
    }

    private ResponseEntity<RabbitAdminErrorResponseDto> badRequest(List<String> details) {
        return ResponseEntity.badRequest().body(new RabbitAdminErrorResponseDto(INVALID_INPUT, details));
    }
}

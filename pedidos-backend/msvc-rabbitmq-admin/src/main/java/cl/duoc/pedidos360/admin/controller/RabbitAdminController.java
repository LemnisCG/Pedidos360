package cl.duoc.pedidos360.admin.controller;

import cl.duoc.pedidos360.admin.dto.BindingDto;
import cl.duoc.pedidos360.admin.dto.RabbitAdminOperationResultDto;
import cl.duoc.pedidos360.admin.dto.RabbitResourceNameRules;
import cl.duoc.pedidos360.admin.dto.ResourceDto;
import cl.duoc.pedidos360.admin.service.RabbitAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * API REST de administración de RabbitMQ.
 *
 *   POST   /api/admin/queues                  crea una cola durable           -> 201
 *   DELETE /api/admin/queues/{name}           elimina una cola                -> 204
 *   POST   /api/admin/exchanges               crea un exchange directo        -> 201
 *   DELETE /api/admin/exchanges/{name}        elimina un exchange             -> 204
 *   POST   /api/admin/bindings                vincula una cola a un exchange  -> 201
 *   DELETE /api/admin/bindings?queueName=&exchangeName=&routingKey=
 *                                             elimina un binding              -> 204
 *
 * Los datos inválidos responden 400 y los rechazos de RabbitMQ 409, ambos con un JSON de error.
 */
@RestController
@RequestMapping("/api/admin")
public class RabbitAdminController {

    private static final String INVALID_NAME_MESSAGE = "El nombre contiene caracteres inválidos";
    private static final String NAME_TOO_LONG_MESSAGE = "El nombre no puede superar los 255 caracteres";

    private final RabbitAdminService rabbitAdminService;

    public RabbitAdminController(RabbitAdminService rabbitAdminService) {
        this.rabbitAdminService = rabbitAdminService;
    }

    @PostMapping("/queues")
    public ResponseEntity<RabbitAdminOperationResultDto> createQueue(@Valid @RequestBody ResourceDto dto) {
        rabbitAdminService.createQueue(dto.getName());
        return created("Cola creada: " + dto.getName());
    }

    @DeleteMapping("/queues/{name}")
    public ResponseEntity<Void> deleteQueue(
            @PathVariable
            @Pattern(regexp = RabbitResourceNameRules.VALID_NAME_PATTERN, message = INVALID_NAME_MESSAGE)
            @Size(max = RabbitResourceNameRules.MAX_NAME_LENGTH, message = NAME_TOO_LONG_MESSAGE)
            String name
    ) {
        rabbitAdminService.deleteQueue(name);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/exchanges")
    public ResponseEntity<RabbitAdminOperationResultDto> createExchange(@Valid @RequestBody ResourceDto dto) {
        rabbitAdminService.createExchange(dto.getName());
        return created("Exchange creado: " + dto.getName());
    }

    @DeleteMapping("/exchanges/{name}")
    public ResponseEntity<Void> deleteExchange(
            @PathVariable
            @Pattern(regexp = RabbitResourceNameRules.VALID_NAME_PATTERN, message = INVALID_NAME_MESSAGE)
            @Size(max = RabbitResourceNameRules.MAX_NAME_LENGTH, message = NAME_TOO_LONG_MESSAGE)
            String name
    ) {
        rabbitAdminService.deleteExchange(name);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bindings")
    public ResponseEntity<RabbitAdminOperationResultDto> createBinding(@Valid @RequestBody BindingDto dto) {
        rabbitAdminService.createBinding(dto.getQueueName(), dto.getExchangeName(), dto.getRoutingKey());
        return created("Binding creado entre " + dto.getQueueName() + " y " + dto.getExchangeName());
    }

    /** Un binding se identifica por cola, exchange y routing key; van como parámetros porque DELETE no lleva cuerpo. */
    @DeleteMapping("/bindings")
    public ResponseEntity<Void> deleteBinding(
            @RequestParam
            @Pattern(regexp = RabbitResourceNameRules.VALID_NAME_PATTERN, message = "El nombre de la cola contiene caracteres inválidos")
            @Size(max = RabbitResourceNameRules.MAX_NAME_LENGTH, message = "El nombre de la cola no puede superar los 255 caracteres")
            String queueName,
            @RequestParam
            @Pattern(regexp = RabbitResourceNameRules.VALID_NAME_PATTERN, message = "El nombre del exchange contiene caracteres inválidos")
            @Size(max = RabbitResourceNameRules.MAX_NAME_LENGTH, message = "El nombre del exchange no puede superar los 255 caracteres")
            String exchangeName,
            @RequestParam
            @Pattern(regexp = RabbitResourceNameRules.VALID_NAME_PATTERN, message = "La routing key contiene caracteres inválidos")
            @Size(max = RabbitResourceNameRules.MAX_NAME_LENGTH, message = "La routing key no puede superar los 255 caracteres")
            String routingKey
    ) {
        rabbitAdminService.deleteBinding(queueName, exchangeName, routingKey);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<RabbitAdminOperationResultDto> created(String message) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new RabbitAdminOperationResultDto(message));
    }
}

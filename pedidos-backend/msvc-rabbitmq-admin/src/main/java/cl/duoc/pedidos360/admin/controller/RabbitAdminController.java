package cl.duoc.pedidos360.admin.controller;

import cl.duoc.pedidos360.admin.dto.BindingDto;
import cl.duoc.pedidos360.admin.dto.ResourceDto;
import cl.duoc.pedidos360.admin.service.RabbitAdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class RabbitAdminController {

    private final RabbitAdminService rabbitAdminService;

    public RabbitAdminController(RabbitAdminService rabbitAdminService) {
        this.rabbitAdminService = rabbitAdminService;
    }

    @PostMapping("/queues")
    public ResponseEntity<String> createQueue(@Valid @RequestBody ResourceDto dto) {
        rabbitAdminService.createQueue(dto.getName());
        return ResponseEntity.ok("Cola creada: " + dto.getName());
    }

    @DeleteMapping("/queues/{name}")
    public ResponseEntity<String> deleteQueue(@PathVariable String name) {
        rabbitAdminService.deleteQueue(name);
        return ResponseEntity.ok("Cola eliminada: " + name);
    }

    @PostMapping("/exchanges")
    public ResponseEntity<String> createExchange(@Valid @RequestBody ResourceDto dto) {
        rabbitAdminService.createExchange(dto.getName());
        return ResponseEntity.ok("Exchange creado: " + dto.getName());
    }

    @DeleteMapping("/exchanges/{name}")
    public ResponseEntity<String> deleteExchange(@PathVariable String name) {
        rabbitAdminService.deleteExchange(name);
        return ResponseEntity.ok("Exchange eliminado: " + name);
    }

    @PostMapping("/bindings")
    public ResponseEntity<String> createBinding(@Valid @RequestBody BindingDto dto) {
        rabbitAdminService.createBinding(dto.getQueueName(), dto.getExchangeName(), dto.getRoutingKey());
        return ResponseEntity.ok("Binding creado entre " + dto.getQueueName() + " y " + dto.getExchangeName());
    }

    @DeleteMapping("/bindings")
    public ResponseEntity<String> deleteBinding(@Valid @RequestBody BindingDto dto) {
        rabbitAdminService.deleteBinding(dto.getQueueName(), dto.getExchangeName(), dto.getRoutingKey());
        return ResponseEntity.ok("Binding eliminado entre " + dto.getQueueName() + " y " + dto.getExchangeName());
    }
}

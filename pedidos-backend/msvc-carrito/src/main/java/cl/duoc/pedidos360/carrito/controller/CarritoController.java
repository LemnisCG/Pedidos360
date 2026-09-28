package cl.duoc.pedidos360.carrito.controller;

import cl.duoc.pedidos360.carrito.model.CarritoItem;
import cl.duoc.pedidos360.carrito.repository.CarritoRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Expone las operaciones del carrito del usuario autenticado.
 * X-User-Id es una cabecera interna confiable que agrega api-gateway después de validar el JWT.
 */
@RestController
public class CarritoController {

    private final CarritoRepository repository;

    public CarritoController(CarritoRepository repository) {
        this.repository = repository;
    }

    /** DTO mínimo para agregar o actualizar la cantidad de un producto. */
    public record ItemRequest(
            @NotNull Long productoId,
            @Min(1) int cantidad
    ) {
    }

    /** Devuelve solamente los ítems pertenecientes al usuario autenticado. */
    @GetMapping("/api/carrito")
    public List<CarritoItem> all(@RequestHeader("X-User-Id") UUID userId) {
        return repository.findAllByUsuarioId(userId);
    }

    /** Agrega un producto o incrementa su cantidad cuando ya estaba en el carrito. */
    @PostMapping("/api/carrito")
    public CarritoItem add(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody ItemRequest request
    ) {
        CarritoItem item = repository.findByUsuarioIdAndProductoId(userId, request.productoId())
                .orElseGet(CarritoItem::new);

        boolean nuevo = item.getId() == null;
        item.setUsuarioId(userId);
        item.setProductoId(request.productoId());
        item.setCantidad(nuevo ? request.cantidad() : item.getCantidad() + request.cantidad());
        return repository.save(item);
    }

    /** Reemplaza la cantidad actual de un producto ya existente en el carrito. */
    @PutMapping("/api/carrito/{productoId}")
    public CarritoItem update(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable Long productoId,
            @Valid @RequestBody ItemRequest request
    ) {
        CarritoItem item = repository.findByUsuarioIdAndProductoId(userId, productoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ítem no encontrado"));
        item.setCantidad(request.cantidad());
        return repository.save(item);
    }

    /** Elimina un producto individual del carrito del usuario actual. */
    @DeleteMapping("/api/carrito/{productoId}")
    public ResponseEntity<Void> delete(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable Long productoId
    ) {
        repository.findByUsuarioIdAndProductoId(userId, productoId).ifPresent(repository::delete);
        return ResponseEntity.noContent().build();
    }

    /**
     * Endpoint interno usado después de una compra exitosa.
     * No se publica a través de api-gateway.
     */
    @Transactional
    @DeleteMapping("/internal/carrito/{usuarioId}")
    public ResponseEntity<Void> clearInternal(@PathVariable UUID usuarioId) {
        repository.deleteAllByUsuarioId(usuarioId);
        return ResponseEntity.noContent().build();
    }
}

package cl.duoc.carrito.controller;

import cl.duoc.carrito.dto.CarritoRequest;
import cl.duoc.carrito.dto.CarritoResponse;
import cl.duoc.carrito.service.CarritoService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/carrito")
public class CarritoController {

    private final CarritoService carritoService;

    public CarritoController(CarritoService carritoService) {
        this.carritoService = carritoService;
    }

    @GetMapping
    public ResponseEntity<List<CarritoResponse>> listarCarrito() {

        return ResponseEntity.ok(
                carritoService.listarCarrito()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<CarritoResponse> buscarPorId(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                carritoService.buscarPorId(id)
        );
    }

    @PostMapping
    public ResponseEntity<CarritoResponse> agregarProducto(
            @Valid @RequestBody CarritoRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(carritoService.agregarProducto(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CarritoResponse> actualizarProducto(
            @PathVariable Long id,
            @Valid @RequestBody CarritoRequest request) {

        return ResponseEntity.ok(
                carritoService.actualizarProducto(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarProducto(
            @PathVariable Long id) {

        carritoService.eliminarProducto(id);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> vaciarCarrito() {

        carritoService.vaciarCarrito();

        return ResponseEntity.noContent().build();
    }
}

package cl.duoc.pedidos360.producto.controller;

import cl.duoc.pedidos360.producto.dto.StockRequest;
import cl.duoc.pedidos360.producto.model.Producto;
import cl.duoc.pedidos360.producto.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Endpoints públicos de lectura y endpoint interno para el descuento de stock. */
@RestController
public class ProductoController {

    private final ProductoService service;

    public ProductoController(ProductoService service) {
        this.service = service;
    }

    @GetMapping("/api/productos")
    public List<Producto> all() {
        return service.all();
    }

    @GetMapping("/api/productos/{id}")
    public Producto one(@PathVariable Long id) {
        return service.one(id);
    }

    /**
     * Solo se invoca dentro de la red Docker desde msvc-pago.
     * api-gateway no publica rutas /internal/**.
     */
    @PostMapping("/internal/productos/descontar-stock")
    public ResponseEntity<Void> descontar(@Valid @RequestBody List<StockRequest> items) {
        service.descontar(items);
        return ResponseEntity.noContent().build();
    }
}

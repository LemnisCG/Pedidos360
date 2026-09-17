package cl.duoc.pedidos360.producto.controller;

import cl.duoc.pedidos360.producto.model.Producto;
import cl.duoc.pedidos360.producto.service.ProductoService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
@CrossOrigin(origins = "http://localhost:4200")
public class ProductoController {
    private final ProductoService service;

    public ProductoController(ProductoService service) {
        this.service = service;
    }

    @GetMapping
    public List<Producto> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public Producto buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    public Producto crear(@RequestBody Producto p) {
        p.setId(null);
        return service.guardar(p);
    }

    @PutMapping("/{id}")
    public Producto actualizar(@PathVariable Long id, @RequestBody Producto p) {
        return service.actualizar(id, p);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }

    /*
     * Endpoint interno usado por msvc-orden al confirmar una compra.
     * Ejemplo: [{"productoId": 8, "cantidad": 3}]
     */
    @PostMapping("/descontar-stock")
    public List<Producto> descontarStock(@RequestBody List<ProductoService.StockItem> items) {
        try {
            return service.descontarStock(items);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }
}

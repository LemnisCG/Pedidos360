package cl.duoc.catalogo.controller;

import cl.duoc.catalogo.dto.ProductoRequest;
import cl.duoc.catalogo.dto.ProductoResponse;
import cl.duoc.catalogo.service.CatalogoService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalogo")
public class CatalogoController {

    private final CatalogoService catalogoService;

    public CatalogoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    // GET - LISTAR PRODUCTOS
    @GetMapping
    public ResponseEntity<List<ProductoResponse>> listarProductos() {

        return ResponseEntity.ok(
                catalogoService.listarProductos());
    }

    // GET - BUSCAR PRODUCTO POR ID
    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponse> buscarPorId(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                catalogoService.buscarPorId(id));
    }

    // POST - CREAR PRODUCTO
    @PostMapping
    public ResponseEntity<ProductoResponse> crearProducto(
            @Valid @RequestBody ProductoRequest request) {

        ProductoResponse producto = catalogoService.crearProducto(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(producto);
    }

    // PUT - ACTUALIZAR PRODUCTO
    @PutMapping("/{id}")
    public ResponseEntity<ProductoResponse> actualizarProducto(
            @PathVariable Long id,
            @Valid @RequestBody ProductoRequest request) {

        return ResponseEntity.ok(
                catalogoService.actualizarProducto(id, request));
    }

    // DELETE - ELIMINAR PRODUCTO
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarProducto(
            @PathVariable Long id) {

        catalogoService.eliminarProducto(id);

        return ResponseEntity.noContent().build();
    }
}
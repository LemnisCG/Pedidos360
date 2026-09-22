package cl.duoc.pedidos360.orden.controller;

import cl.duoc.pedidos360.orden.dto.OrdenRequest;
import cl.duoc.pedidos360.orden.model.Orden;
import cl.duoc.pedidos360.orden.service.OrdenService;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/ordenes")
@CrossOrigin(origins = "http://localhost:4200")
public class OrdenController {
    private final OrdenService service;

    public OrdenController(OrdenService service) {
        this.service = service;
    }

    @PostMapping
    public Orden crear(@RequestBody OrdenRequest r) {
        return service.crear(r);
    }

    @GetMapping("/{id}")
    public Orden buscar(@PathVariable UUID id) {
        return service.buscar(id);
    }
}
package cl.duoc.pedidos360.pago.controller;

import cl.duoc.pedidos360.pago.dto.PagoRequest;
import cl.duoc.pedidos360.pago.model.Pago;
import cl.duoc.pedidos360.pago.repository.PagoRepository;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/pagos")
@CrossOrigin(origins = "http://localhost:4200")
public class PagoController {
    private final PagoRepository repo;

    public PagoController(PagoRepository repo) { this.repo = repo; }

    // Pago simulado para fines académicos: NO procesa datos bancarios reales.
    @PostMapping
    public Pago pagar(@RequestBody PagoRequest r) {
        return repo.save(Pago.builder()
                .id(UUID.randomUUID())
                .monto(r.monto())
                .moneda(r.moneda())
                .metodo(r.metodo())
                .ultimos4(r.ultimos4())
                .estado("APROBADO")
                .creadoEn(Instant.now())
                .build());
    }

    @GetMapping("/{id}")
    public Pago buscar(@PathVariable UUID id) {
        return repo.findById(id).orElseThrow();
    }
}

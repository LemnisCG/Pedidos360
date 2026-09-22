package cl.duoc.pedidos360.orden.service;

import cl.duoc.pedidos360.orden.dto.OrdenRequest;
import cl.duoc.pedidos360.orden.model.Orden;
import cl.duoc.pedidos360.orden.model.OrdenItem;
import cl.duoc.pedidos360.orden.repository.OrdenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
public class OrdenService {
    private final OrdenRepository repo;
    private final EmailService emailService;
    private final RestClient productoClient;

    public OrdenService(
            OrdenRepository repo,
            EmailService emailService,
            @Value("${services.producto.url:http://localhost:8082}") String productoUrl
    ) {
        this.repo = repo;
        this.emailService = emailService;
        this.productoClient = RestClient.builder().baseUrl(productoUrl).build();
    }

    public Orden crear(OrdenRequest r) {
        if (r.items() == null || r.items().isEmpty()) {
            throw new IllegalArgumentException("La orden debe contener al menos un producto");
        }

        /*
         * Antes de guardar la orden se solicita a msvc-producto descontar el stock.
         * Si no hay existencias suficientes, el microservicio responde con error y
         * la orden no se crea con un inventario incorrecto.
         */
        List<StockItemRequest> stockItems = r.items().stream()
                .map(i -> new StockItemRequest(i.productoId(), i.cantidad()))
                .toList();

        productoClient.post()
                .uri("/api/productos/descontar-stock")
                .body(stockItems)
                .retrieve()
                .toBodilessEntity();

        List<OrdenItem> items = r.items().stream()
                .map(i -> OrdenItem.builder()
                        .productoId(i.productoId())
                        .nombre(i.nombre())
                        .precioUnitario(i.precioUnitario())
                        .cantidad(i.cantidad())
                        .imagenUrl(i.imagenUrl())
                        .codigoJuego(codigo())
                        .build())
                .toList();

        BigDecimal total = items.stream()
                .map(i -> i.getPrecioUnitario().multiply(BigDecimal.valueOf(i.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Orden orden = Orden.builder()
                .id(UUID.randomUUID())
                .numeroOrden(String.valueOf(1000000 + new Random().nextInt(8999999)))
                .fecha(Instant.now())
                .estado("COMPLETADA")
                .metodoPago("Visa •••• 4242")
                .total(total)
                .clienteEmail(r.clienteEmail())
                .direccionEnvio(r.direccionEnvio())
                .confirmacionEmailEnviada(false)
                .items(items)
                .build();

        // Primero guardamos la orden. Luego intentamos el correo sin arriesgar la compra.
        orden = repo.save(orden);
        boolean enviado = emailService.enviarConfirmacionPago(orden);
        orden.setConfirmacionEmailEnviada(enviado);
        return repo.save(orden);
    }

    public Orden buscar(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada"));
    }

    private String codigo() {
        return UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase()
                .replaceAll("(.{4})(?=.)", "$1-");
    }

    private record StockItemRequest(Long productoId, Integer cantidad) {}
}

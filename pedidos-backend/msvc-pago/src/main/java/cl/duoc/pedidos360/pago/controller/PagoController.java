package cl.duoc.pedidos360.pago.controller;

import cl.duoc.pedidos360.pago.model.Pedido;
import cl.duoc.pedidos360.pago.model.PedidoItem;
import cl.duoc.pedidos360.pago.repository.PedidoRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Orquesta la compra: consulta precios confiables, descuenta stock, persiste el pedido,
 * solicita el correo de confirmación y limpia el carrito.
 */
@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final PedidoRepository repository;
    private final RestClient http = RestClient.create();
    private final SecureRandom random = new SecureRandom();
    private final String productoUrl;
    private final String carritoUrl;
    private final String envioUrl;

    public PagoController(
            PedidoRepository repository,
            @Value("${services.producto-url}") String productoUrl,
            @Value("${services.carrito-url}") String carritoUrl,
            @Value("${services.envio-url}") String envioUrl
    ) {
        this.repository = repository;
        this.productoUrl = productoUrl;
        this.carritoUrl = carritoUrl;
        this.envioUrl = envioUrl;
    }

    /** Producto y cantidad elegidos por el cliente. */
    public record ItemRequest(
            @NotNull Long productoId,
            @Min(1) int cantidad
    ) {
    }

    /** Datos mínimos del checkout; los datos completos de tarjeta nunca se persisten. */
    public record CheckoutRequest(
            @NotBlank String direccion,
            @NotBlank String numeroTarjeta,
            @NotBlank String expiracion,
            @NotBlank String codigoSeguridad,
            @NotEmpty List<@Valid ItemRequest> items
    ) {
    }

    /** Contrato interno para descontar inventario. */
    public record StockRequest(Long productoId, int cantidad) {
    }

    /** Vista del producto devuelta por msvc-producto. */
    public record Producto(
            Long id,
            String nombre,
            String descripcion,
            long precio,
            int stock,
            String categoria,
            String imagenUrl,
            String fechaLanzamiento
    ) {
    }

    /** Producto incluido en el correo de confirmación. */
    public record EnvioItem(String nombre, int cantidad, long precio, String imagenUrl) {
    }

    /** Contrato interno enviado a msvc-envio. */
    public record EnvioRequest(
            UUID pedidoId,
            String numeroPedido,
            UUID usuarioId,
            String nombre,
            String email,
            String direccion,
            long total,
            List<EnvioItem> items
    ) {
    }

    public record EnvioResult(boolean emailEnviado, UUID envioId) {
    }

    /** Ejecuta el checkout autenticado y devuelve el pedido ya persistido. */
    @PostMapping("/checkout")
    @Transactional
    public Pedido checkout(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Name") String nombre,
            @Valid @RequestBody CheckoutRequest request
    ) {
        String cardDigits = request.numeroTarjeta().replaceAll("\\D", "");
        if (cardDigits.length() < 12 || cardDigits.length() > 19) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Número de tarjeta inválido");
        }

        Pedido pedido = new Pedido();
        pedido.setNumeroPedido(generateOrderNumber());
        pedido.setUsuarioId(userId);
        pedido.setEmail(email);
        pedido.setNombreUsuario(nombre);
        pedido.setDireccion(request.direccion().trim());
        pedido.setMetodoPago("Tarjeta");
        pedido.setTarjetaUltimos4(cardDigits.substring(cardDigits.length() - 4));
        pedido.setEstado("COMPLETADO");

        long total = 0L;
        List<StockRequest> stockRequests = new ArrayList<>();
        List<EnvioItem> emailItems = new ArrayList<>();

        // Los precios se vuelven a consultar en backend para no confiar en valores manipulables del frontend.
        for (ItemRequest item : request.items()) {
            Producto producto = http.get()
                    .uri(productoUrl + "/api/productos/" + item.productoId())
                    .retrieve()
                    .body(Producto.class);

            if (producto == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Producto no disponible");
            }

            PedidoItem pedidoItem = new PedidoItem();
            pedidoItem.setProductoId(producto.id());
            pedidoItem.setNombre(producto.nombre());
            pedidoItem.setCantidad(item.cantidad());
            pedidoItem.setPrecio(producto.precio());
            pedidoItem.setImagenUrl(producto.imagenUrl());
            pedidoItem.setCodigoDigital(generateDigitalCode());
            pedido.add(pedidoItem);

            total += producto.precio() * item.cantidad();
            stockRequests.add(new StockRequest(producto.id(), item.cantidad()));
            emailItems.add(new EnvioItem(
                    producto.nombre(), item.cantidad(), producto.precio(), producto.imagenUrl()
            ));
        }

        // El dueño del inventario valida nuevamente y descuenta con bloqueo pesimista.
        http.post()
                .uri(productoUrl + "/internal/productos/descontar-stock")
                .body(stockRequests)
                .retrieve()
                .toBodilessEntity();

        pedido.setTotal(total);
        pedido = repository.save(pedido);

        // El pedido no se pierde si SMTP falla: se conserva emailEnviado=false para poder reintentar.
        try {
            EnvioResult result = http.post()
                    .uri(envioUrl + "/internal/envios/confirmacion")
                    .body(new EnvioRequest(
                            pedido.getId(),
                            pedido.getNumeroPedido(),
                            userId,
                            nombre,
                            email,
                            request.direccion(),
                            total,
                            emailItems
                    ))
                    .retrieve()
                    .body(EnvioResult.class);
            pedido.setEmailEnviado(result != null && result.emailEnviado());
        } catch (Exception ignored) {
            pedido.setEmailEnviado(false);
        }
        pedido = repository.save(pedido);

        // Una compra confirmada deja el carrito vacío, pero un fallo al limpiar no invalida el pago.
        try {
            http.delete()
                    .uri(carritoUrl + "/internal/carrito/" + userId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ignored) {
            // Se mantiene el pedido confirmado; el usuario todavía puede vaciar el carrito manualmente.
        }

        return pedido;
    }

    /** Historial del usuario autenticado, ordenado desde la compra más reciente. */
    @GetMapping("/mis-pedidos")
    public List<Pedido> mine(@RequestHeader("X-User-Id") UUID userId) {
        return repository.findAllByUsuarioIdOrderByCreadoEnDesc(userId);
    }

    /** Detalle de un pedido, siempre comprobando que pertenezca al usuario autenticado. */
    @GetMapping("/mis-pedidos/{id}")
    public Pedido one(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id
    ) {
        return repository.findByIdAndUsuarioId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
    }

    /** Número legible para mostrar al usuario sin exponer el UUID interno. */
    private String generateOrderNumber() {
        return "PED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /** Genera códigos digitales legibles evitando caracteres visualmente ambiguos. */
    private String generateDigitalCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 14; i++) {
            if (i > 0 && i % 4 == 0) {
                code.append('-');
            }
            code.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
        }
        return code.toString();
    }
}

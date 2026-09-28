package cl.duoc.pedidos360.catalogo.controller;

import cl.duoc.pedidos360.catalogo.model.CatalogoConfig;
import cl.duoc.pedidos360.catalogo.repository.CatalogoConfigRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Compone el catálogo consumiendo msvc-producto por HTTP.
 * De esta forma no se comparte directamente la base de datos de productos.
 */
@RestController
public class CatalogoController {

    private static final String DEFAULT_LABEL = "CATÁLOGO DIGITAL";
    private static final String DEFAULT_TITLE = "TU PRÓXIMA AVENTURA COMIENZA AQUÍ";
    private static final String DEFAULT_SUBTITLE =
            "Clásicos inolvidables y nuevos mundos, reunidos en un catálogo preparado para jugar.";

    private final CatalogoConfigRepository repository;
    private final RestClient restClient = RestClient.create();
    private final String productoUrl;

    public CatalogoController(
            CatalogoConfigRepository repository,
            @Value("${services.producto-url}") String productoUrl
    ) {
        this.repository = repository;
        this.productoUrl = productoUrl;
    }

    /** Devuelve en una sola respuesta el banner y los productos actuales. */
    @GetMapping("/api/catalogo")
    public Map<String, Object> catalogo() {
        Object productos = restClient.get()
                .uri(productoUrl + "/api/productos")
                .retrieve()
                .body(Object.class);

        CatalogoConfig banner = repository.findById(1L)
                .orElseGet(CatalogoController::defaultBanner);

        return Map.of(
                "banner", banner,
                "productos", productos == null ? List.of() : productos
        );
    }

    /** Crea una configuración inicial solo cuando la base está vacía. */
    @Bean
    CommandLineRunner seed(CatalogoConfigRepository repo) {
        return args -> {
            if (!repo.existsById(1L)) {
                repo.save(defaultBanner());
            }
        };
    }

    private static CatalogoConfig defaultBanner() {
        return new CatalogoConfig(DEFAULT_LABEL, DEFAULT_TITLE, DEFAULT_SUBTITLE);
    }
}

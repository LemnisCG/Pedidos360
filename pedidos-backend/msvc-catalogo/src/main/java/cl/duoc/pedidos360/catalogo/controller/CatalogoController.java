package cl.duoc.pedidos360.catalogo.controller;

import cl.duoc.pedidos360.catalogo.model.CatalogoConfig;
import cl.duoc.pedidos360.catalogo.repository.CatalogoConfigRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

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

    /**
     * Reemplaza los textos del banner. El api-gateway solo deja pasar esta ruta
     * cuando el access token trae el App Role "Administrador".
     */
    @PutMapping("/api/catalogo/banner")
    public CatalogoConfig updateCatalogBanner(@RequestBody CatalogBannerUpdateRequest request) {
        if (isBlank(request.etiqueta()) || isBlank(request.titulo()) || isBlank(request.subtitulo())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "La etiqueta, el título y el subtítulo son obligatorios.");
        }

        CatalogoConfig banner = repository.findById(1L).orElseGet(CatalogoController::defaultBanner);
        banner.replaceBannerTexts(request.etiqueta().trim(), request.titulo().trim(), request.subtitulo().trim());
        return repository.save(banner);
    }

    public record CatalogBannerUpdateRequest(String etiqueta, String titulo, String subtitulo) {
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
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

package cl.duoc.pedidos360.gateway.proxy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;

/** Proxy explícito que conserva método, path, query, cuerpo y cabeceras útiles. */
@Controller
public class ProxyController {
    private final WebClient webClient = WebClient.builder().build();
    private final Map<String, String> targets;

    public ProxyController(
            @Value("${pedidos360.services.usuario}") String usuario,
            @Value("${pedidos360.services.producto}") String producto,
            @Value("${pedidos360.services.carrito}") String carrito,
            @Value("${pedidos360.services.catalogo}") String catalogo,
            @Value("${pedidos360.services.pago}") String pago,
            @Value("${pedidos360.services.envio}") String envio) {
        targets = Map.of(
                "/api/auth", usuario,
                "/api/usuarios", usuario,
                "/api/productos", producto,
                "/api/carrito", carrito,
                "/api/catalogo", catalogo,
                "/api/pagos", pago,
                "/api/envios", envio
        );
    }

    @RequestMapping("/health")
    public Mono<Void> health(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.OK);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = "{\"status\":\"UP\",\"service\":\"api-gateway\"}".getBytes();
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }

    @RequestMapping("/api/**")
    public Mono<Void> proxy(ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getRawPath();
        String base = targets.entrySet().stream()
                .filter(entry -> path.startsWith(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);

        if (base == null) {
            exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
            return exchange.getResponse().setComplete();
        }

        String query = exchange.getRequest().getURI().getRawQuery();
        URI target = URI.create(base + path + (query == null ? "" : "?" + query));

        WebClient.RequestBodySpec request = webClient
                .method(exchange.getRequest().getMethod())
                .uri(target)
                .headers(headers -> copyRequestHeaders(exchange.getRequest().getHeaders(), headers));

        return request.body(BodyInserters.fromDataBuffers(exchange.getRequest().getBody()))
                .exchangeToMono(response -> {
                    exchange.getResponse().setStatusCode(response.statusCode());
                    response.headers().asHttpHeaders().forEach((name, values) -> {
                        if (!name.equalsIgnoreCase(HttpHeaders.TRANSFER_ENCODING)
                                && !name.equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH)) {
                            exchange.getResponse().getHeaders().put(name, values);
                        }
                    });
                    return exchange.getResponse().writeWith(
                            response.bodyToFlux(byte[].class)
                                    .map(bytes -> exchange.getResponse().bufferFactory().wrap(bytes))
                    );
                });
    }

    private void copyRequestHeaders(HttpHeaders source, HttpHeaders target) {
        source.forEach((name, values) -> {
            if (!name.equalsIgnoreCase(HttpHeaders.HOST)
                    && !name.equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH)
                    && !name.equalsIgnoreCase(HttpHeaders.TRANSFER_ENCODING)) {
                target.put(name, values);
            }
        });
    }
}

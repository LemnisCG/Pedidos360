package cl.duoc.pedidos360.bff.application;

import cl.duoc.pedidos360.bff.interfaces.rest.dto.PerfilSyncRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class PerfilClientService {

    private final RestClient restClient;

    // Spring inyecta la URL desde tu application.yaml y construye el cliente
    public PerfilClientService(@Value("${microservicios.perfil.url}") String msPerfilUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(msPerfilUrl)
                .build();
    }

    public Map<String, Object> sincronizarPerfil(String oid, String email, String nombre) {
        PerfilSyncRequest requestBody = new PerfilSyncRequest(oid, email, nombre);

        // Hace un POST a http://localhost:8081/api/perfiles/sync
        return restClient.post()
                .uri("/api/v1/perfiles/sync")
                .body(requestBody)
                .retrieve()
                .body(Map.class); // Mapea la respuesta de ms-perfil a un Map generico
    }
}
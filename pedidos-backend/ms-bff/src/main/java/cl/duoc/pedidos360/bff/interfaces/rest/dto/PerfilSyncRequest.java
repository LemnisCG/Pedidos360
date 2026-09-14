package cl.duoc.pedidos360.bff.interfaces.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PerfilSyncRequest(
        @JsonProperty("idProvider") String idProvider,
        String email,
        String nombre
) {}
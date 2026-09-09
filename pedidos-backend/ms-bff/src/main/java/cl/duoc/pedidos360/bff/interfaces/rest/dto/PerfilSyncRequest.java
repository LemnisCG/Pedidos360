package cl.duoc.pedidos360.bff.interfaces.rest.dto;

public record PerfilSyncRequest (String azureOid, String email, String nombre) {}
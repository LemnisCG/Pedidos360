package cl.duoc.pedidos360.admin.dto;

/** Reglas compartidas para validar nombres de colas, exchanges y routing keys en toda la API. */
public final class RabbitResourceNameRules {

    public static final String VALID_NAME_PATTERN = "^[a-zA-Z0-9._-]+$";

    /** RabbitMQ no admite nombres de más de 255 bytes. */
    public static final int MAX_NAME_LENGTH = 255;

    private RabbitResourceNameRules() {
    }
}

package cl.duoc.pedidos360.pago.exception;

/** RabbitMQ no confirmó la publicación de un evento: no está garantizado que haya llegado a alguna cola. */
public class PublicacionDeEventoFallidaException extends RuntimeException {

    public PublicacionDeEventoFallidaException(String message) {
        super(message);
    }

    public PublicacionDeEventoFallidaException(String message, Throwable cause) {
        super(message, cause);
    }
}

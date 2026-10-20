package cl.duoc.pedidos360.pago.exception;

import java.util.UUID;

/**
 * Llegó un aviso para un pedido que aún no está en la base de datos.
 * Puede pasar si el aviso llega antes de que termine la transacción del checkout, por eso se reintenta.
 */
public class PedidoTodaviaNoRegistradoException extends RuntimeException {

    public PedidoTodaviaNoRegistradoException(UUID pedidoId) {
        super("El pedido " + pedidoId + " todavía no está registrado");
    }
}

package cl.duoc.pedidos360.envio.listener;

import cl.duoc.pedidos360.envio.publisher.ReintentoDeMensajesPublisher;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;

import java.io.IOException;
import java.util.Map;

/**
 * Manejo de confirmaciones común a todos los consumidores de msvc-envio (ACK manual).
 *
 * Decisión ante cada mensaje:
 *   - Procesado sin errores: ACK, el mensaje se elimina de la cola.
 *   - Error recuperable (falla temporal de infraestructura) con reintentos disponibles: se publica una copia
 *     en la cola de reintento del caso de uso y se hace ACK del original; la copia vuelve tras la espera (TTL).
 *   - Error no recuperable (datos inválidos) o reintentos agotados: NACK sin requeue, el mensaje va a la DLQ.
 *
 * Cada consumidor concreto solo define qué hace con el evento y qué errores considera recuperables.
 */
public abstract class ConsumidorConReintentosYDeadLetterQueue<E> {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final ReintentoDeMensajesPublisher reintentoPublisher;
    private final String reintentoQueueName;
    private final int maximoReintentos;
    private final String headerNumeroReintento;

    protected ConsumidorConReintentosYDeadLetterQueue(
            ReintentoDeMensajesPublisher reintentoPublisher,
            String reintentoQueueName,
            int maximoReintentos,
            String headerNumeroReintento
    ) {
        this.reintentoPublisher = reintentoPublisher;
        this.reintentoQueueName = reintentoQueueName;
        this.maximoReintentos = maximoReintentos;
        this.headerNumeroReintento = headerNumeroReintento;
    }

    /** Lógica de negocio del caso de uso. Debe lanzar una excepción si el mensaje no se pudo procesar. */
    protected abstract void procesar(E event) throws Exception;

    /** Indica si vale la pena reintentar: el mismo error podría no repetirse más tarde. */
    protected abstract boolean esErrorRecuperable(Exception e);

    /** Identificador de negocio que se muestra en los logs, por ejemplo el número de pedido. */
    protected abstract String describir(E event);

    protected final void procesarConConfirmacionManual(
            E event,
            Channel channel,
            long deliveryTag,
            Map<String, Object> headers
    ) throws IOException {
        int reintentosRealizados = leerReintentosRealizados(headers);
        String identificador = event == null ? "desconocido" : describir(event);
        log.info("Mensaje recibido para pedido: {} (reintentos realizados: {})", identificador, reintentosRealizados);

        try {
            procesar(event);
            channel.basicAck(deliveryTag, false);
            log.info("ACK enviado para pedido: {}", identificador);
        } catch (Exception e) {
            boolean recuperable = esErrorRecuperable(e);
            if (recuperable && reintentosRealizados < maximoReintentos) {
                reintentar(event, identificador, reintentosRealizados + 1, e, channel, deliveryTag);
            } else {
                log.error("Error {} procesando pedido {} tras {} reintentos. Enviando a DLQ: {}",
                        recuperable ? "recuperable" : "no recuperable", identificador, reintentosRealizados, e.getMessage());
                channel.basicNack(deliveryTag, false, false);
            }
        }
    }

    /** Publica el evento en la cola de reintento y confirma el original; si no puede, lo deriva a la DLQ. */
    private void reintentar(
            E event,
            String identificador,
            int numeroReintento,
            Exception causa,
            Channel channel,
            long deliveryTag
    ) throws IOException {
        try {
            reintentoPublisher.publicarParaReintento(reintentoQueueName, event, numeroReintento);
        } catch (AmqpException errorAlPublicar) {
            log.error("No se pudo programar el reintento del pedido {}. Enviando a DLQ: {}",
                    identificador, errorAlPublicar.getMessage());
            channel.basicNack(deliveryTag, false, false);
            return;
        }
        // ACK del original: la copia en la cola de reintento volverá a la cola principal al vencer la espera
        channel.basicAck(deliveryTag, false);
        log.warn("Error recuperable procesando pedido {}. Programado reintento {} de {}: {}",
                identificador, numeroReintento, maximoReintentos, causa.getMessage());
    }

    private int leerReintentosRealizados(Map<String, Object> headers) {
        return headers.get(headerNumeroReintento) instanceof Number numero ? numero.intValue() : 0;
    }

    protected static boolean estaVacio(String value) {
        return value == null || value.isBlank();
    }
}

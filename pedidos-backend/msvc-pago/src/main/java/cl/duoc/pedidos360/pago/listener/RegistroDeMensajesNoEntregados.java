package cl.duoc.pedidos360.pago.listener;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Registra en logs un mensaje leído desde una DLQ (cola de origen, motivo, headers y contenido) y lo confirma.
 * Lo usan los consumidores de DLQ de cada caso de uso.
 */
@Component
public class RegistroDeMensajesNoEntregados {

    public void registrarYConfirmar(Logger log, Message message, Channel channel) throws IOException {
        MessageProperties properties = message.getMessageProperties();
        String cuerpo = new String(message.getBody(), StandardCharsets.UTF_8);

        // RabbitMQ agrega el header x-death con el historial; la primera entrada es la más reciente.
        List<Map<String, ?>> historialDeMuertes = properties.getXDeathHeader();
        Object colaOrigen = "desconocida";
        Object motivo = "desconocido";
        if (historialDeMuertes != null && !historialDeMuertes.isEmpty()) {
            Map<String, ?> ultimaMuerte = historialDeMuertes.get(0);
            colaOrigen = ultimaMuerte.get("queue");
            motivo = ultimaMuerte.get("reason");
        }

        log.error("Mensaje no entregado registrado desde la DLQ. colaOrigen={}, motivo={}, headers={}, cuerpo={}",
                colaOrigen, motivo, properties.getHeaders(), cuerpo);

        // ACK: el mensaje ya quedó registrado en logs y se retira de la DLQ
        channel.basicAck(properties.getDeliveryTag(), false);
    }
}

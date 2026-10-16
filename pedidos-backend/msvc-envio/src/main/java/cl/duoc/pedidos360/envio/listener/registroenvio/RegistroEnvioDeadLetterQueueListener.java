package cl.duoc.pedidos360.envio.listener.registroenvio;

import cl.duoc.pedidos360.envio.config.RabbitMQConfig;
import cl.duoc.pedidos360.envio.listener.RegistroDeMensajesNoEntregados;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Consumidor de la DLQ del registro de envíos: deja en logs cada envío que no se pudo registrar.
 * Usa un conversor sin transformación para poder registrar también los mensajes con JSON inválido.
 */
@Component
public class RegistroEnvioDeadLetterQueueListener {

    private static final Logger log = LoggerFactory.getLogger(RegistroEnvioDeadLetterQueueListener.class);

    private final RegistroDeMensajesNoEntregados registroDeMensajesNoEntregados;

    public RegistroEnvioDeadLetterQueueListener(RegistroDeMensajesNoEntregados registroDeMensajesNoEntregados) {
        this.registroDeMensajesNoEntregados = registroDeMensajesNoEntregados;
    }

    @RabbitListener(
            queues = "${rabbitmq.queue.envio-dlq}",
            messageConverter = RabbitMQConfig.RAW_MESSAGE_CONVERTER_BEAN_NAME
    )
    public void onMensajeNoEntregado(Message message, Channel channel) throws IOException {
        registroDeMensajesNoEntregados.registrarYConfirmar(log, message, channel);
    }
}

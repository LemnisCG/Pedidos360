package cl.duoc.pedidos360.envio.listener.notificacioncorreo;

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
 * Consumidor de la DLQ de notificaciones: deja en logs cada correo que no se pudo enviar.
 * Usa un conversor sin transformación para poder registrar también los mensajes con JSON inválido.
 */
@Component
public class NotificacionCorreoDeadLetterQueueListener {

    private static final Logger log = LoggerFactory.getLogger(NotificacionCorreoDeadLetterQueueListener.class);

    private final RegistroDeMensajesNoEntregados registroDeMensajesNoEntregados;

    public NotificacionCorreoDeadLetterQueueListener(RegistroDeMensajesNoEntregados registroDeMensajesNoEntregados) {
        this.registroDeMensajesNoEntregados = registroDeMensajesNoEntregados;
    }

    @RabbitListener(
            queues = "${rabbitmq.queue.notificacion-correo-dlq}",
            messageConverter = RabbitMQConfig.RAW_MESSAGE_CONVERTER_BEAN_NAME
    )
    public void onMensajeNoEntregado(Message message, Channel channel) throws IOException {
        registroDeMensajesNoEntregados.registrarYConfirmar(log, message, channel);
    }
}

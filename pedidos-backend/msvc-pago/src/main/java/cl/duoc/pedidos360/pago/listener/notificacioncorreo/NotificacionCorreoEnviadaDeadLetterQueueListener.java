package cl.duoc.pedidos360.pago.listener.notificacioncorreo;

import cl.duoc.pedidos360.pago.config.RabbitMQConfig;
import cl.duoc.pedidos360.pago.listener.RegistroDeMensajesNoEntregados;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Consumidor de la DLQ de avisos de correo enviado: deja en logs cada aviso que no se pudo aplicar a un pedido.
 * Usa un conversor sin transformación para poder registrar también los mensajes con JSON inválido.
 */
@Component
public class NotificacionCorreoEnviadaDeadLetterQueueListener {

    private static final Logger log = LoggerFactory.getLogger(NotificacionCorreoEnviadaDeadLetterQueueListener.class);

    private final RegistroDeMensajesNoEntregados registroDeMensajesNoEntregados;

    public NotificacionCorreoEnviadaDeadLetterQueueListener(RegistroDeMensajesNoEntregados registroDeMensajesNoEntregados) {
        this.registroDeMensajesNoEntregados = registroDeMensajesNoEntregados;
    }

    @RabbitListener(
            queues = "${rabbitmq.queue.correo-enviado-dlq}",
            messageConverter = RabbitMQConfig.RAW_MESSAGE_CONVERTER_BEAN_NAME
    )
    public void onMensajeNoEntregado(Message message, Channel channel) throws IOException {
        registroDeMensajesNoEntregados.registrarYConfirmar(log, message, channel);
    }
}

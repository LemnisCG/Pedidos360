
package cl.duoc.pedidos360.envio.kafka;

import cl.duoc.pedidos360.envio.model.Envio;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class EnvioKafkaPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(EnvioKafkaPublisher.class);

    private static final String TOPIC = "envios.eventos";

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper;

    public EnvioKafkaPublisher(
            KafkaTemplate<String, String> kafka,
            ObjectMapper mapper
    ) {
        this.kafka = kafka;
        this.mapper = mapper;
    }

    public void publicarRegistrado(Envio envio) {

        if (envio == null ||
                envio.getId() == null ||
                envio.getPedidoId() == null) {
            throw new IllegalArgumentException(
                    "El envio debe estar registrado antes de publicar el evento"
            );
        }

        Map<String, Object> evento = new LinkedHashMap<>();

        evento.put("eventId", UUID.randomUUID().toString());
        evento.put("eventType", "ENVIO_REGISTRADO");
        evento.put("schemaVersion", 1);
        evento.put("occurredAt", Instant.now().toString());
        evento.put("pedidoId", envio.getPedidoId().toString());
        evento.put("envioId", envio.getId().toString());
        evento.put("numeroPedido", envio.getNumeroPedido());
        evento.put("estadoNuevo", envio.getEstado());

        try {
            String mensaje = mapper.writeValueAsString(evento);
            String clave = envio.getPedidoId().toString();

            kafka.send(TOPIC, clave, mensaje)
                    .whenComplete((resultado, error) -> {

                        if (error != null) {
                            log.error(
                                    "Error publicando evento Kafka. Pedido: {}",
                                    envio.getNumeroPedido(),
                                    error
                            );
                            return;
                        }

                        log.info(
                                "Evento ENVIO_REGISTRADO publicado. Pedido: {}, Particion: {}, Offset: {}",
                                envio.getNumeroPedido(),
                                resultado.getRecordMetadata().partition(),
                                resultado.getRecordMetadata().offset()
                        );
                    });

        } catch (Exception e) {
            log.error(
                    "No se pudo preparar el evento de envio. Pedido: {}",
                    envio.getNumeroPedido(),
                    e
            );
        }
    }
}

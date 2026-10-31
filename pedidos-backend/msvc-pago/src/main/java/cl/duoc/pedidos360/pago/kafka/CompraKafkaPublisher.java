package cl.duoc.pedidos360.pago.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class CompraKafkaPublisher {
    private static final Logger log = LoggerFactory.getLogger(CompraKafkaPublisher.class);
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper;

    public CompraKafkaPublisher(KafkaTemplate<String, String> kafka, ObjectMapper mapper) {
        this.kafka = kafka;
        this.mapper = mapper;
    }

    public void publicar(UUID id, String numeroPedido, long total, int items) {
        try {
            String eventoId = UUID.randomUUID().toString();
            var envelope = Map.of("eventId", eventoId, "eventType", "PEDIDO_COMPLETADO",
                "schemaVersion", 1, "occurredAt", Instant.now().toString(),
                "pedidoId", id.toString(), "numeroPedido", numeroPedido, "total", total, "cantidadItems", items);
            String data = mapper.writeValueAsString(envelope);
            for (String topic : new String[]{"pedidos.eventos", "pagos.eventos"}) {
                kafka.send(topic, id.toString(), data).whenComplete((result, error) -> {
                    if (error != null) log.error("Kafka {} fallo para {}: {}", topic, numeroPedido, error.toString());
                    else log.info("Kafka {} partition={} offset={} pedido={}", topic,
                        result.getRecordMetadata().partition(), result.getRecordMetadata().offset(), numeroPedido);
                });
            }
        } catch (Exception e) {
            log.error("No se pudo preparar evento Kafka pedido={}", numeroPedido, e);
        }
    }
}

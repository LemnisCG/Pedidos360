package cl.duoc.pedidos360.catalogo.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class CompraStreamConsumer {
    private static final Logger log = LoggerFactory.getLogger(CompraStreamConsumer.class);

    @KafkaListener(topics = "pedidos.eventos", groupId = "catalogo-analitica", concurrency = "3")
    public void onEvento(ConsumerRecord<String, String> record) {
        log.info("STREAM catalogo: topic={} partition={} offset={} key={} payload={}",
                record.topic(), record.partition(), record.offset(), record.key(), record.value());
    }
}

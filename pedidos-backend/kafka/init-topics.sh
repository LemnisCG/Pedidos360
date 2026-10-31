#!/bin/bash
set -euo pipefail
BOOTSTRAP="kafka-1:9092,kafka-2:9092,kafka-3:9092"

# Wait for all three brokers: a topic with replication factor 3 requires all of them.
echo "Esperando los tres brokers de Apache Kafka..."
for attempt in $(seq 1 90); do
  if kafka-broker-api-versions --bootstrap-server "$BOOTSTRAP" >/dev/null 2>&1; then
    brokers=$(kafka-topics --bootstrap-server "$BOOTSTRAP" --describe --topic __consumer_offsets 2>/dev/null || true)
    # The admin client can connect before the cluster is fully registered; creation below will retry.
    break
  fi
  sleep 3
done

create_topic() {
  local topic="$1"
  local success=0
  for attempt in $(seq 1 60); do
    if kafka-topics --bootstrap-server "$BOOTSTRAP" --create --if-not-exists \
      --topic "$topic" --partitions 3 --replication-factor 3; then
      success=1
      break
    fi
    echo "Reintentando crear $topic ($attempt/60)..."
    sleep 5
  done
  if [ "$success" -ne 1 ]; then
    echo "ERROR: no se pudo crear $topic" >&2
    return 1
  fi
  kafka-topics --bootstrap-server "$BOOTSTRAP" --describe --topic "$topic"
}

create_topic pedidos.eventos
create_topic pagos.eventos
create_topic envios.eventos
echo "Los tres topicos estan inicializados."

#!/usr/bin/env bash
set -euo pipefail

BOOTSTRAP="kafka-1:9092,kafka-2:9092,kafka-3:9092"
TOPICS=("pedidos.eventos" "pagos.eventos" "envios.eventos")

# Espera a que el clúster responda antes de inicializar los tópicos.
echo "Esperando a Apache Kafka..."
ready=false
for attempt in $(seq 1 90); do
  if kafka-topics --bootstrap-server "$BOOTSTRAP" --list >/dev/null 2>&1; then
    ready=true
    break
  fi
  echo "Kafka no disponible ($attempt/90). Reintentando en 3 segundos..."
  sleep 3
done

if [[ "$ready" != true ]]; then
  echo "ERROR: Kafka no respondió después de los reintentos." >&2
  exit 1
fi

create_topic() {
  local topic="$1"
  for attempt in $(seq 1 60); do
    if kafka-topics \
      --bootstrap-server "$BOOTSTRAP" \
      --create \
      --if-not-exists \
      --topic "$topic" \
      --partitions 3 \
      --replication-factor 3; then
      echo "Tópico $topic disponible:"
      kafka-topics --bootstrap-server "$BOOTSTRAP" --describe --topic "$topic"
      return 0
    fi
    echo "Reintentando crear $topic ($attempt/60)..."
    sleep 5
  done
  echo "ERROR: no se pudo crear el tópico $topic" >&2
  return 1
}

for topic in "${TOPICS[@]}"; do
  create_topic "$topic"
done

kafka-topics --bootstrap-server "$BOOTSTRAP" --list
echo "Inicialización finalizada correctamente (salida 0)."

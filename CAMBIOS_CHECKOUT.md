# Cambios aplicados en Checkout y Kafka

## Frontend
- Se eliminó el texto **"Confirmación de compra simulada · Sin cargo real"**.
- Se eliminó la etiqueta **"✓ Pago simulado aprobado"** de la pantalla de confirmación.
- Se eliminó la etiqueta **"🔒 Compra simulada"** del encabezado del checkout.
- Se dejó un solo método de pago con formulario completo de tarjeta.
- Se agregaron validaciones para número de tarjeta, expiración, código de seguridad, país y código postal.
- Se dejó el campo **Comuna** como selector dependiente de la **Región**.
- Se retiraron las opciones deshabilitadas de Mercado Pago y BancoEstado.
- Se mejoró la redacción visual para que el checkout se vea más profesional.

## Backend / correo
- Se cambió el método de pago persistido a **Tarjeta**.
- Se ajustó el correo HTML para mostrar **Pago confirmado**.

## Kafka / Docker
- Se corrigió `pedidos-backend/kafka/init-topics.sh`.
- Se forzó la normalización de saltos de línea (`CRLF` -> `LF`) antes de ejecutar el script dentro del contenedor.
- Se dejó `kafka-ui` esperando la finalización correcta de `kafka-init`, para que los tópicos ya existan al abrir la interfaz.
- Se agregaron reglas `.gitattributes` para conservar los `.sh` con fin de línea `LF`.

## Verificación sugerida
Ejecutar desde la raíz del proyecto. La arquitectura actual usa un solo ZooKeeper. Para migraciones con volúmenes anteriores, consultar el README y hacer respaldo previamente.
```powershell
# limpiar el init anterior si quedó fallando
docker compose rm -f kafka-init

# volver a levantar Kafka y el inicializador
docker compose up -d zookeeper-1 kafka-1 kafka-2 kafka-3 kafka-init kafka-ui

# revisar el inicializador
docker compose logs kafka-init --tail=100

# listar tópicos manualmente si quieres validar
docker compose exec kafka-1 kafka-topics --bootstrap-server kafka-1:9092 --list
```

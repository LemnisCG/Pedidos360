# Pedidos360

Plataforma de compras de videojuegos desarrollada con **Angular**, **Java 21 / Spring Boot**, **PostgreSQL**, **RabbitMQ** y **Apache Kafka**. Se ejecuta desde la raíz mediante un único archivo `docker-compose.yml`.

## Requisitos

- Docker Desktop con Docker Compose v2.
- Puertos locales disponibles para el frontend y las herramientas de administración.

## Inicio rápido

1. Copiar `.env.example` a `.env` en la **raíz** del proyecto y completar las credenciales necesarias (SMTP, PostgreSQL y Grafana). No subir `.env` a GitHub.
2. Abrir una terminal en la raíz `Pedidos360/`.
3. Ejecutar:

```powershell
Antes ejecutar el primer comando pra restablecer el entorno, incluyendo datos persistentes
docker compose down --volumes --rmi all --remove-orphans
docker compose up -d
docker compose ps -a

para ver los logs
docker compose up --build
```

El contenedor `kafka-init` crea automáticamente `pedidos.eventos`, `pagos.eventos` y `envios.eventos` (3 particiones y 3 réplicas cada uno). Que `kafka-init` termine en **Exited (0)** es normal: su trabajo se completó.

## Servicios locales

| Servicio | Acceso | Descripción |
| --- | --- | --- |
| Angular (frontend) | http://localhost:4200 | Tienda y compras |
| API Gateway | http://localhost:8080 | Entrada al backend |
| Kafka UI | http://localhost:8088 | Brokers, tópicos y mensajes |
| RabbitMQ Management | http://localhost:15672 | Exchanges, colas y consumidores |
| Grafana | http://localhost:3000 | Dashboard de Kafka |
| Prometheus | http://localhost:9090 | Métricas y estado de targets |
| Administración RabbitMQ | http://localhost:8090 | API administrativa |

Las credenciales de Grafana se definen en `.env` mediante `GRAFANA_ADMIN_USER` y `GRAFANA_ADMIN_PASSWORD`. Las de RabbitMQ dependen de la configuración del broker en Compose. Cambiar estas contraseñas para despliegues expuestos a redes externas.

## Verificar mensajería y monitoreo

```powershell
docker compose logs kafka-init --tail=50
docker compose exec kafka-1 kafka-topics --bootstrap-server kafka-1:9092 --list
docker compose logs msvc-pago msvc-envio --tail=60
docker compose ps -a kafka-exporter prometheus grafana
```

Realizar una compra y observar los eventos en Kafka UI. En Prometheus revisar `/targets` y consultar `kafka_brokers`; en Grafana abrir **Dashboards → Pedidos360 → Pedidos360 | Apache Kafka**. Kafka es el canal de streaming; RabbitMQ conserva el procesamiento operacional de envío, correo y reintentos.

## Operación

```powershell
docker compose logs -f msvc-pago msvc-envio
docker compose down
```

`docker compose down` conserva los volúmenes; **evitar `down -v`** si se quieren conservar pedidos y bases de datos. Para recompilar un servicio: `docker compose up -d --build msvc-envio`.

## Organización

- `pedidos-backend/`: microservicios Spring Boot, infraestructura Kafka y observabilidad.
- `pedidos360-frontend/`: frontend Angular, Dockerfile y Nginx.
- `pedidos-docs/`: informes académicos sobre RabbitMQ y Kafka.
- `.env.example`: plantilla de configuración sin secretos.
- `docker-compose.yml`: despliegue unificado desde la raíz.

## Documentación

Consultar `pedidos-docs/Mensajeria_con_Apache_Kafka_en_Pedidos360.pdf` y el informe de RabbitMQ para la arquitectura, conceptos, pruebas y limitaciones.

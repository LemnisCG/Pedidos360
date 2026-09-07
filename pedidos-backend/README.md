# Backend Pedidos360

`pedidos-backend` es el directorio del monorepo backend. Cada microservicio es un proyecto Maven independiente.

## ms-perfil

El módulo usa Java 21, Spring Boot 3.3.13, Spring Data JPA con Hibernate 6 y PostgreSQL 16.

```text
ms-perfil/
├── pom.xml
├── mvnw
├── mvnw.cmd
├── .mvn/
└── src/
    ├── main/
    │   ├── java/cl/duoc/pedidos360/perfil/
    │   │   ├── interfaces/rest/       # Presentación: controladores REST
    │   │   ├── application/           # Aplicación: servicios y casos de uso
    │   │   ├── domain/model/           # Negocio: entidades y reglas del dominio
    │   │   └── infrastructure/        # Datos: repositorios y adaptadores
    │   └── resources/application.properties
    └── test/
```

## Comandos

Desde la carpeta `ms-perfil`:

```bash
./mvnw test
./mvnw spring-boot:run
```

Variables de conexión disponibles: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JPA_DDL_AUTO` y `SERVER_PORT`.

## Ejecución con Docker Compose

Docker Compose levanta PostgreSQL, pgAdmin y `ms-perfil` en una red privada. Copia `.env.example` a `.env` y ajusta las credenciales antes de iniciar:

```bash
cp .env.example .env
docker compose up --build
```

Servicios disponibles:

| Servicio | Dirección | Uso |
|---|---|---|
| `ms-perfil` | `http://localhost:8081` | API REST |
| PostgreSQL de `ms-perfil` | `localhost:5433` | Conexiones desde el equipo/pgAdmin |
| pgAdmin | `http://localhost:5050` | Administración visual |

En pgAdmin usa `admin@pedidos360.local` y `admin` para iniciar sesión. Para registrar la base de `ms-perfil`, crea un servidor con host `postgres-perfil`, puerto `5432`, base `pedidos360_perfil`, usuario `postgres` y la contraseña definida en `.env`. Dentro de Docker se usa `postgres-perfil:5432`; desde tu máquina se usa `localhost:5433`.

Cada nuevo microservicio debe tener su propio servicio PostgreSQL, por ejemplo `postgres-pedidos`, sus propias variables `PEDIDOS_DB_*` y su propio volumen. No debe reutilizar `postgres-perfil` ni `perfil_postgres_data`.

Para detener los servicios:

```bash
docker compose down
```

Para eliminar también los datos persistidos:

```bash
docker compose down -v
```
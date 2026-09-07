# Backend Pedidos360

`pedidos-backend` es el agregador Maven del monorepo backend. Cada microservicio debe vivir en su propio módulo, por ejemplo `ms-perfil`.

## ms-perfil

El módulo usa Java 21, Spring Boot 3.3.13, Spring Data JPA con Hibernate 6 y PostgreSQL 16.

```text
ms-perfil/
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

Desde esta carpeta:

```bash
sh mvnw -pl ms-perfil -am test
sh mvnw -pl ms-perfil spring-boot:run
```

Variables de conexión disponibles: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JPA_DDL_AUTO` y `SERVER_PORT`.
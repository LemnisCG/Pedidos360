<div align="center">

# ⚙️ Pedidos360 Backend

**API Gateway + microservicios Spring Boot para una tienda digital de videojuegos**

![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)
![OAuth](https://img.shields.io/badge/OAuth%202.0-PKCE-4B5563)
![JWT](https://img.shields.io/badge/JWT-Seguridad-000000?logo=jsonwebtokens&logoColor=white)

</div>

## Descripción

El backend de **Pedidos360** usa una arquitectura de microservicios. El frontend consume una sola entrada pública (`api-gateway:8080`) y el gateway se encarga de validar JWT, aplicar CORS y enrutar cada petición hacia el servicio correspondiente.

## Microservicios

| Servicio | Puerto | Responsabilidad |
|---|---:|---|
| `api-gateway` | 8080 | JWT, CORS y enrutamiento |
| `msvc-usuario` | 8081 | Registro local, BCrypt, OAuth y JWT |
| `msvc-producto` | 8082 | Productos, precios y stock transaccional |
| `msvc-carrito` | 8083 | Carrito persistente por usuario |
| `msvc-catalogo` | 8084 | Catálogo y contenido editorial |
| `msvc-pago` | 8085 | Checkout, pedidos, historial y códigos digitales |
| `msvc-envio` | 8086 | Dirección de envío y notificación SMTP |

Cada microservicio de dominio tiene su propia base PostgreSQL, `Dockerfile`, Maven Wrapper y `application.yml`.

## Ejecución

Los archivos de infraestructura viven en esta misma carpeta:

```text
pedidos360-backend/
├── .env
├── .env.example
├── compose-pedidos360.yml
└── docker-compose.yml
```

Desde `pedidos360-backend`:

```bash
docker compose -f compose-pedidos360.yml up --build
```

Gateway:

```text
http://localhost:8080/health
```

> No subas `.env` con secretos reales a GitHub. Usa `.env.example` como plantilla.

## OAuth local: callbacks

Registra exactamente estas URI en cada proveedor:

```text
Google     http://localhost:8080/api/auth/oauth2/callback/google
Facebook   http://localhost:8080/api/auth/oauth2/callback/facebook
Discord    http://localhost:8080/api/auth/oauth2/callback/discord
Microsoft  http://localhost:8080/api/auth/oauth2/callback/microsoft
```

### Facebook

En **Facebook Login > Settings** mantén habilitados **Client OAuth Login** y **Web OAuth Login**. Durante desarrollo local utiliza como *Valid OAuth Redirect URI* el callback del backend mostrado arriba. Si el navegador termina en `http://localhost:4200/#_=_`, Facebook está regresando al frontend sin completar el callback del Gateway; revisa la URI registrada y elimina callbacks locales antiguos que apunten directamente a `:4200`.

El backend canjea el `code` por un access token y consulta `id,name,email`. Si Facebook no entrega email, Pedidos360 crea un identificador local estable para no bloquear el inicio de sesión.

### Diagnóstico

Para revisar únicamente autenticación:

```powershell
docker compose logs -f msvc-usuario api-gateway
```

Los errores del proveedor quedan registrados sin imprimir access tokens ni client secrets.

### Microsoft Entra External ID

Microsoft se configura desde `pedidos360-backend/.env`, igual que los demás
proveedores OAuth:

```env
MICROSOFT_CLIENT_ID=
MICROSOFT_TENANT_ID=92aabe28-d724-4ac3-a20c-45088143bf29
MICROSOFT_CLIENT_SECRET=
MICROSOFT_AUTH_URL=https://pedidos360auth.ciamlogin.com/92aabe28-d724-4ac3-a20c-45088143bf29/oauth2/v2.0/authorize
MICROSOFT_TOKEN_URL=https://pedidos360auth.ciamlogin.com/92aabe28-d724-4ac3-a20c-45088143bf29/oauth2/v2.0/token
MICROSOFT_USERINFO_URL=https://graph.microsoft.com/oidc/userinfo
MICROSOFT_SCOPE=openid profile email offline_access
```

En **Microsoft Entra → App registrations → Authentication** agrega como
plataforma **Web** la siguiente Redirect URI:

```text
http://localhost:8080/api/auth/oauth2/callback/microsoft
```

El `Client ID`, `Client Secret`, `Tenant ID`, `AUTH_URL` y `TOKEN_URL` deben
pertenecer al mismo tenant. `msvc-usuario` valida esta coherencia antes de iniciar
el flujo para evitar errores por mezclar directorios distintos.


### Correcciones de autenticación (V10)

- Facebook usa Authorization Code + PKCE de extremo a extremo: el `code_verifier` guardado con `state` se reenvía en el canje del código.
- El registro local devuelve mensajes JSON legibles; si el correo ya pertenecía únicamente a una cuenta OAuth, puede añadirse una contraseña local sin perder el vínculo social.
- Un correo que ya posee contraseña local continúa respondiendo `409 Conflict`, pero con un mensaje claro para el frontend.


## OAuth: diagnóstico rápido

- **Microsoft**: para el callback Web del backend, `MICROSOFT_CLIENT_SECRET` debe contener el **Valor** de un secreto vigente. El `Client ID` y el `Tenant ID` por sí solos no completan el canje del authorization code.
- **Facebook**: Pedidos360 conserva `state` + `code_verifier` en PostgreSQL para PKCE. Además vincula la identidad externa en `oauth_identities`, por lo que un mismo correo puede utilizar más de un proveedor sin sobrescribir el vínculo anterior.
- Los callbacks locales esperados son `http://localhost:8080/api/auth/oauth2/callback/<proveedor>`.
- Para ver el motivo real de un fallo usa `docker compose logs -f msvc-usuario api-gateway`. Los secretos y access tokens se redactan en los mensajes de diagnóstico.

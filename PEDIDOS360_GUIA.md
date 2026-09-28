# Pedidos360 — Guía de configuración y ejecución

## 1. Requisitos

- Docker Desktop + Docker Compose
- Node.js **24.13.x o superior dentro de la rama 24** (Angular 21 LTS también soporta Node 22.12+)
- npm
- Puertos libres: `4200`, `8080–8086`, `5433–5438`

## 2. Estructura

```text
Pedidos360/
├── README.md
├── PEDIDOS360_GUIA.md
├── AUDITORIA_TECNICA.md
├── pedidos360-backend/
│   ├── .env
│   ├── .env.example
│   ├── compose-pedidos360.yml
│   ├── docker-compose.yml
│   ├── api-gateway/       # :8080
│   ├── msvc-usuario/      # :8081
│   ├── msvc-producto/     # :8082
│   ├── msvc-carrito/      # :8083
│   ├── msvc-catalogo/     # :8084
│   ├── msvc-pago/         # :8085
│   └── msvc-envio/        # :8086
└── pedidos360-frontend/
    ├── public/assets/games/
    ├── public/assets/icons/
    ├── public/assets/providers/
    └── src/app/
```

## 3. Levantar Backend

Toda la configuración de Docker y secretos quedó dentro de `pedidos360-backend`.

```powershell
cd pedidos360-backend

docker compose -f compose-pedidos360.yml down
docker compose -f compose-pedidos360.yml up --build

o

docker compose down
docker compose up --build

```

Detener sin borrar bases de datos:

```powershell
docker compose -f compose-pedidos360.yml down
```

No uses `down -v` salvo que quieras eliminar los volúmenes PostgreSQL.

Comprobar Gateway:

```text
http://localhost:8080/health
```

## 4. Levantar Frontend 

```powershell

cd pedidos360-frontend
npm install
npm run dev
```


## 5. Registro local

El modal usa **controles HTML nativos** y validación desde Angular. Esto evita
problemas de foco o escritura cuando se abre el formulario sobre el catálogo.
Para crear una cuenta solicita:

- Nombre
- Apellido
- Correo
- Contraseña (mínimo 8 caracteres)
- Repetir contraseña

El login local solicita correo y contraseña. Los errores se muestran dentro del mismo modal.

## 6. OAuth — callbacks locales

Configura exactamente estas URLs en cada proveedor:

```text
Google:    http://localhost:8080/api/auth/oauth2/callback/google
Facebook:  http://localhost:8080/api/auth/oauth2/callback/facebook
Discord:   http://localhost:8080/api/auth/oauth2/callback/discord
Microsoft: http://localhost:8080/api/auth/oauth2/callback/microsoft
```

### Facebook

Para Facebook Login web, deja activados **Client OAuth Login** y **Web OAuth Login**. Usa exactamente el callback anterior.

Esta versión usa Graph API `v26.0` y el Authorization Code del lado servidor. `FACEBOOK_PKCE_ENABLED` queda en `false` porque el flujo manual documentado por Meta para Facebook Login web canjea el código con `client_secret` en backend.

Si el proveedor no devuelve `email`, Pedidos360 regresa al modal con un mensaje claro en lugar de mostrar `Whitelabel Error Page`.

### Microsoft Entra External ID

El error `AADSTS50011` significa que la URI enviada por Pedidos360 no coincide con una Redirect URI registrada.

En **Microsoft Entra → App registrations → tu aplicación → Authentication** agrega, bajo plataforma **Web**:

```text
http://localhost:8080/api/auth/oauth2/callback/microsoft
```

Debe coincidir exactamente. Como el código se canjea desde `msvc-usuario`, configura también un **Client Secret** y colócalo en:

```env
MICROSOFT_CLIENT_SECRET=...
```

La configuración de Microsoft queda completamente externalizada en `.env`:

```env
MICROSOFT_CLIENT_ID=
MICROSOFT_TENANT_ID=92aabe28-d724-4ac3-a20c-45088143bf29
MICROSOFT_CLIENT_SECRET=
MICROSOFT_AUTH_URL=https://pedidos360auth.ciamlogin.com/92aabe28-d724-4ac3-a20c-45088143bf29/oauth2/v2.0/authorize
MICROSOFT_TOKEN_URL=https://pedidos360auth.ciamlogin.com/92aabe28-d724-4ac3-a20c-45088143bf29/oauth2/v2.0/token
MICROSOFT_USERINFO_URL=https://graph.microsoft.com/oidc/userinfo
MICROSOFT_SCOPE=openid profile email offline_access
```

`MICROSOFT_AUTH_URL` y `MICROSOFT_TOKEN_URL` podrían construirse en código, pero
se mantienen como variables para usar el mismo patrón de configuración que con
Google, Facebook y Discord y para evitar URLs OAuth hardcodeadas.

> Importante: `MICROSOFT_CLIENT_ID` y `MICROSOFT_CLIENT_SECRET` deben pertenecer
> a una aplicación registrada en el mismo tenant indicado por
> `MICROSOFT_TENANT_ID`. No mezcles credenciales de otro directorio de Azure con
> los endpoints `pedidos360auth.ciamlogin.com`.

## 7. Variables OAuth

Archivo: `pedidos360-backend/.env`

```env
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
FACEBOOK_CLIENT_ID=
FACEBOOK_CLIENT_SECRET=
DISCORD_CLIENT_ID=
DISCORD_CLIENT_SECRET=
MICROSOFT_CLIENT_ID=
MICROSOFT_TENANT_ID=92aabe28-d724-4ac3-a20c-45088143bf29
MICROSOFT_CLIENT_SECRET=
MICROSOFT_AUTH_URL=https://pedidos360auth.ciamlogin.com/92aabe28-d724-4ac3-a20c-45088143bf29/oauth2/v2.0/authorize
MICROSOFT_TOKEN_URL=https://pedidos360auth.ciamlogin.com/92aabe28-d724-4ac3-a20c-45088143bf29/oauth2/v2.0/token
MICROSOFT_USERINFO_URL=https://graph.microsoft.com/oidc/userinfo
MICROSOFT_SCOPE=openid profile email offline_access
```

No publiques valores reales en GitHub.

## 8. SMTP

```env
MAIL_ENABLED=true
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=cuenta-remitente@gmail.com
MAIL_PASSWORD=CONTRASENA_DE_APLICACION
MAIL_FROM=cuenta-remitente@gmail.com
```

## 9. Flujo de autenticación

```text
Frontend Angular
      ↓
API Gateway :8080
      ↓
msvc-usuario
  ↙             ↘
Cuenta local     OAuth externo
BCrypt + JWT     Google / Facebook / Discord / Microsoft
      \           /
       JWT interno Pedidos360
              ↓
        rutas protegidas
```

## 10. Flujo de compra

```text
Tienda → Producto → Carrito → Checkout
                         ↓
                    msvc-pago
                  ↙      ↓      ↘
             producto  carrito  envío/SMTP
                         ↓
                    Mis pedidos
```

## Corrección V8: frontend y Facebook

- Angular se mantiene en 21.2.x para ser compatible con Node 24.13.x.
- Se retiraron `vitest` y `jsdom` del frontend porque eran dependencias de pruebas no utilizadas y provocaban conflicto de peer dependencies durante `npm install`.
- El modal de autenticación usa inputs HTML nativos y `FormData`, evitando reinicios o bloqueos de escritura por estado de formularios.
- Los botones de proveedores muestran el icono a la izquierda y el texto centrado.
- Facebook usa un canje de código específico y consulta Graph API con `access_token`; el callback local debe apuntar al API Gateway, no directamente a Angular.


## Corrección Facebook PKCE y registro local — V10

Si Facebook autenticaba al usuario pero regresaba al formulario, el log mostraba `No code_verifier specified when a code challenge is provided`. La V10 conserva el `code_verifier` junto al `state` y lo reenvía al endpoint de token de Facebook. Mantén `FACEBOOK_PKCE_ENABLED=true`.

Para reconstruir después del cambio:

```powershell
cd pedidos360-backend
docker compose down
docker compose up --build -d
docker compose logs -f msvc-usuario api-gateway
```

El `409 Conflict` del registro local ahora se muestra con un mensaje legible. Si el correo ya existía únicamente por OAuth y aún no tenía contraseña local, Pedidos360 permite añadir la contraseña y reutilizar la misma cuenta. Si el correo ya posee contraseña local, el 409 se mantiene porque se trata de una cuenta duplicada real.

# Pedidos360 - guía rápida

Pedidos360 mantiene la autenticación existente con Microsoft Entra External ID y utiliza una estructura Angular por `core`, `shared` y `features`.

## Puertos
- Frontend Angular: `4200`
- BFF autenticación: `8080`
- Perfil: `8081`
- Producto: `8082`
- Carrito existente: `8083`
- Orden: `8084`
- Pago: `8085`
- Catálogo anterior: `8086` (se conserva para no eliminar trabajo previo)

## Catálogo
El catálogo principal contiene 13 videojuegos:

1. ASTRO BOT
2. Mario Kart 8 Deluxe
3. Minecraft
4. Street Fighter II Ultra
5. Super Mario Bros. 3
6. Red Dead Redemption 2
7. The Witcher 3: Wild Hunt
8. Super Mario Odyssey
9. Elden Ring
10. God of War Ragnarök
11. Tekken 8
12. The Legend of Zelda: Tears of the Kingdom
13. Hollow Knight

El frontend muestra 5 productos en la primera fila, 5 en la segunda y los últimos 3 centrados en escritorio.

## Levantar frontend
```bash
cd pedidos360-frontend
npm install
npm start
o con el comando: 
ng serve --host 0.0.0.0 --port 4200
```

## Levantar Producto, Orden y Pago con Docker
```bash
cd pedidos-backend
docker compose -f compose-pedidos360.yaml down
docker compose -f compose-pedidos360.yaml up --build
```

## Autenticación y pop-up de usuario
Microsoft Entra External ID se configura en `src/app/core/auth/auth.service.ts`.

Al hacer clic sobre el nombre del usuario en la cabecera se muestra:
- Estado: Sesión Activa
- Nombre del usuario
- Correo obtenido desde la cuenta autenticada

## Checkout y dirección de envío
El checkout agrega un campo **Dirección de envío**. La dirección y el correo del usuario se almacenan junto con la orden.

## Correo REAL de confirmación
`msvc-orden` incluye envío de correo por SMTP mediante Spring Mail. El **destinatario** se obtiene automáticamente del correo de la sesión Microsoft Entra.

Por seguridad, el proyecto no contiene contraseñas de correo. Para habilitar el envío real:

1. En `pedidos-backend`, copia `.env.example` como `.env`.
2. Completa una cuenta remitente SMTP real.
3. Mantén `MAIL_ENABLED=true`.
4. Reconstruye `msvc-orden` con Docker Compose.

Ejemplo:
```env
MAIL_ENABLED=true
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=tu-cuenta-remitente@gmail.com
MAIL_PASSWORD=tu-contrasena-de-aplicacion
MAIL_FROM=tu-cuenta-remitente@gmail.com
```

> Para Gmail normalmente debes usar una contraseña de aplicación. Para Microsoft 365/Outlook, la organización debe permitir el método de envío SMTP correspondiente. Nunca subas el archivo `.env` con credenciales reales a GitHub.

## Flujo implementado
Tienda -> detalle del juego -> carrito -> checkout -> pago simulado -> creación de orden -> intento de correo real -> detalle con códigos digitales.

> `msvc-pago` sigue siendo una simulación académica y no procesa ni cobra tarjetas reales. El correo, en cambio, sí se envía realmente cuando SMTP está correctamente configurado.

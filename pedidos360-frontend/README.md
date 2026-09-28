<div align="center">

# 🎮 Pedidos360 Frontend

**Tienda digital responsive desarrollada con Angular 21**

![Angular](https://img.shields.io/badge/Angular-21.2-DD0031?logo=angular&logoColor=white)
![TypeScript](https://img.shields.io/badge/TypeScript-5.9-3178C6?logo=typescript&logoColor=white)
![RxJS](https://img.shields.io/badge/RxJS-7.8-B7178C?logo=reactivex&logoColor=white)
![OAuth](https://img.shields.io/badge/OAuth%202.0-PKCE-2563EB)
![JWT](https://img.shields.io/badge/JWT-Auth-111827?logo=jsonwebtokens)

</div>

## Requisitos

- Node.js `^22.12.0` o `^24.0.0`.
- npm 10 o superior.
- Backend Pedidos360 disponible en `http://localhost:8080`.

## Ejecución

```powershell
npm install
npm run dev
```

La aplicación queda disponible en `http://localhost:4200`.

> Si vienes de una instalación Angular 22 anterior, elimina `node_modules`, `.angular` y `package-lock.json` una sola vez antes de ejecutar `npm install`.

## Autenticación

El modal permite registro local e inicio de sesión local. También redirige al API Gateway para autenticación externa con:

- Google
- Facebook
- Discord
- Microsoft

Los formularios usan controles HTML nativos para mantener foco, teclado, autocompletado y compatibilidad con gestores de contraseñas.

Las credenciales OAuth **no viven en Angular**. Los botones Google, Facebook,
Discord y Microsoft solo redirigen al `api-gateway:8080`; el intercambio de
`authorization code`, PKCE y secretos se realiza en `msvc-usuario`.

## Estructura

```text
src/app/
├── core/       # autenticación, modelos y servicios
├── features/   # tienda, producto, carrito, checkout y pedidos
└── shared/     # header, footer, modal y componentes reutilizables
```

<div align="center">

# 🎮 Pedidos360

**Tienda digital de videojuegos · Angular + Spring Boot + PostgreSQL + Docker + OAuth 2.0**

![Angular](https://img.shields.io/badge/Angular-21%20LTS-DD0031?logo=angular&logoColor=white)
![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)

</div>

Pedidos360 combina una SPA Angular responsive con un backend de microservicios. El flujo contempla registro local, proveedores externos, JWT, catálogo, stock, carrito, pago académico, historial y notificaciones SMTP.

- **Frontend:** `pedidos360-frontend/`
- **Backend:** `pedidos360-backend/`
- **Guía completa:** `PEDIDOS360_GUIA.md`

## Microsoft Entra External ID

La integración Microsoft se configura exclusivamente en `pedidos360-backend/.env`.
El frontend nunca almacena `Client Secret`; solo inicia el flujo a través del API Gateway.
Consulta `PEDIDOS360_GUIA.md` para las variables y la Redirect URI exacta.

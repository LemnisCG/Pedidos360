# Pedidos360 — Arquitectura Base (EP1)

## 📌 Descripción del Proyecto
Este repositorio contiene la arquitectura base del sistema **Pedidos360**, desarrollado como parte de la Evaluación Parcial N°1 de la asignatura *Desarrollo Cloud Native I*. 

El objetivo del proyecto es construir una solución escalable y segura, estructurada mediante microservicios en el backend y preparada para su posterior despliegue e integración en servicios de la nube (AWS EC2, AWS API Gateway) con autenticación basada en IDaaS (Azure AD).

---

## 🛠️ Tecnologías del Proyecto

* **Backend:** Java 21, Spring Boot, Spring Security (OAuth2 Resource Server), Spring Data JPA.
* **IDaaS:** Azure Active Directory (Azure AD).
* **Infraestructura Cloud (Planeada):** AWS EC2, AWS API Gateway, Base de datos Cloud.
* **Frontend:** *En fase de evaluación por el equipo de desarrollo.*

---

## 📂 Organización del Repositorio

La estructura del proyecto está organizada en directorios modulares:

```text
Pedidos360/
│
├── 📁 pedidos360-backend/      # Microservicios / API REST en Spring Boot
│   └── src/main/java/.../
│       ├── config/              # Configuraciones generales (CORS, Beans, etc.)
│       ├── security/            # Filtros BFF y validación JWT (Issuer, Audience, Firma)
│       ├── controllers/         # Endpoints de la API REST
│       ├── services/            # Lógica de negocio de la aplicación
│       ├── repositories/       # Interfaces JPA para persistencia en BD Cloud
│       ├── entities/            # Entidades del modelo de datos
│       ├── dtos/                # Objetos de transferencia de datos (Request/Response)
│       └── exceptions/          # Manejador global de excepciones y respuestas HTTP
│
├── 📁 pedidos360-frontend/     # Cliente Web (Pendiente de inicialización)
└── 📁 pedidos360-docs/         # Documentación técnica, changelogs y diagramas
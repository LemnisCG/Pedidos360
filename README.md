# Pedidos360 — Arquitectura Base (EP1)

## 📌 Descripción del Proyecto
Este repositorio contiene la arquitectura base del sistema **Pedidos360**, desarrollado como parte de la Evaluación Parcial N°1 de la asignatura *Desarrollo Cloud Native I*. 

El objetivo del proyecto es construir una solución escalable y segura, estructurada mediante microservicios en el backend y preparada para su posterior despliegue e integración en servicios de la nube (AWS EC2, AWS API Gateway) con autenticación basada en IDaaS (Azure AD).

---

## 🛠️ Tecnologías del Proyecto

* **Backend:** Java 21, Spring Boot 3.3, Spring Data JPA, Hibernate 6 y PostgreSQL 16.
* **IDaaS:** Azure Active Directory (Azure AD).
* **Infraestructura Cloud (Planeada):** AWS EC2, AWS API Gateway, Base de datos Cloud.
* **Frontend:** *En fase de evaluación por el equipo de desarrollo.*

---

## 📂 Organización del Repositorio

La estructura del proyecto está organizada en directorios modulares:

```text
Pedidos360/
│
├── 📁 pedidos-backend/          # Agregador Maven de microservicios
│   ├── pom.xml                  # Parent y módulos del backend
│   └── 📁 ms-perfil/            # Microservicio de perfiles
│       └── src/main/java/.../
│           ├── interfaces/rest/ # Capa de presentación: API REST
│           ├── application/     # Capa de aplicación: casos de uso
│           ├── domain/model/    # Modelo de negocio
│           └── infrastructure/  # Capa de datos: repositorios JPA
│
├── 📁 pedidos360-frontend/     # Cliente Web (Pendiente de inicialización)
└── 📁 pedidos-docs/            # Documentación técnica, changelogs y diagramas
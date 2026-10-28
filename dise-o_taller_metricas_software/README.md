<p align="center">
  <img src="https://capsule-render.vercel.app/api?type=waving&color=0:7aa2f7,50:bb9af7,100:1a1b26&height=200&section=header&text=GFC%20-%20GLUD%20Fried%20Chicken&fontSize=40&fontColor=c0caf5&animation=fadeIn&fontAlignY=35&desc=Diseno%20de%20Software%20-%20Taller%20Metricas&descSize=16&descAlignY=55&descAlign=50" width="100%">
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.3-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Spring%20Security-6.3-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white" alt="Spring Security">
  <img src="https://img.shields.io/badge/JWT-0.12-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white" alt="JWT">
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Astro-7.3-FF5D01?style=for-the-badge&logo=astro&logoColor=white" alt="Astro">
  <img src="https://img.shields.io/badge/Tailwind%20CSS-v4-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white" alt="Tailwind">
  <img src="https://img.shields.io/badge/H2-2.2-000000?style=for-the-badge&logo=databricks&logoColor=white" alt="H2">
  <img src="https://img.shields.io/badge/Swagger-2.6-85EA2D?style=for-the-badge&logo=openapiinitiative&logoColor=white" alt="Swagger">
</p>

---

> Sistema de pedidos de comida rapida con catalogo personalizable, carrito,
> autenticacion por roles e inventario.

## Sobre el proyecto

**GLUD Fried Chicken (GFC)** es una aplicacion de gestion de comidas rapidas desarrollada para el **Taller de Metricas de Software** de Diseno de Software. El backend expone una API REST con:

- **Catalogo**: comidas con receta de ingredientes (incluidos y opcionales).
- **Pedidos**: creacion con personalizacion (adicionales y retiros) y descuento de stock transaccional.
- **Inventario**: entradas, salidas y alertas de stock por agotarse, con motivo obligatorio.
- **Autenticacion**: JWT con roles `CLIENTE` y `EMPLEADO`.
- **Resumen de pago**: valoracion automatica de comida + adiciones (sin pasarela externa).

Endpoints, cuerpos y codigos de estado en [docs/endpoints-propuestos.md](docs/endpoints-propuestos.md).

## Integrantes

| Nombre | Codigo |
|--------|--------|
| Daniel Mateo Ballesteros Molina | 20221020102 |
| Jan Henrik Sanchez Jerez | 20231020130 |
| Juan Carlos Quintero Rubiano | 20232020172 |
| Juan Diego Peralta Barbosa | 20231020146 |

---

## Arquitectura

Backend con **Clean Architecture** (dominio rico + casos de uso + infraestructura + web), Java 21 y Spring Boot 3.3:

```
backend/
  src/main/java/com/gfc/
    domain/model/       Entidades, enums y reglas de negocio (JPA)
    domain/port/        Contratos del dominio (CodificadorContrasena)
    application/        Casos de uso: Auth, Comida, Pedido, Inventario
    application/exception/
    infrastructure/
      persistence/      Repositorios Spring Data JPA
      security/         JWT, PBKDF2, SecurityConfig, listener
      startup/          DatosDemoSeeder
    web/                Controllers + DTOs + GlobalExceptionHandler
```

| Capa | Tecnologia | Puerto |
|------|-----------|--------|
| Backend | Java 21 + Spring Boot 3.3 + Spring Security + JWT | 8080 |
| Base de datos | H2 en archivo (`backend/data/gfcdb.mv.db`, no requiere servicio externo) | - |
| Frontend | Astro 7.3 + Tailwind CSS v4 (en desarrollo, rama `front`) | 4321 |
| Contenedor | Docker + Docker Compose (Dockerfile multi-stage, build Maven → JRE Alpine) | 8080 |

### Frontend (rama `front`)

La interfaz visual se desarrolla en la rama `front` con **Astro + Tailwind v4**, estetica *paper craft* japonesa en rojo y blanco. Flujos:

1. **Cliente**: navegar el menu, ver detalle de comidas, crear cuenta, iniciar sesion, agregar al carrito, pagar.
2. **Empleado**: dashboard con las comidas mas pedidas, inventario de ingredientes, alertas de stock bajo y registro de entradas/salidas con justificacion.

---

## Ejecucion local

**Requisitos:** JDK 21+, Node.js 20+

```bash
# Backend (H2 en archivo, datos persistentes)
cd backend
mvn spring-boot:run

# Frontend
cd frontend
corepack pnpm install
corepack pnpm dev
```

**Con Docker Compose** (backend + frontend):
```bash
docker compose up --build
```

- Backend: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 Console: http://localhost:8080/h2-console
- Frontend: http://localhost:4321

**Credenciales demo** (sembradas por `DatosDemoSeeder`):

| Rol | Usuario | Clave |
|-----|---------|-------|
| EMPLEADO | `empleado` | `empleado123` |

Los clientes se auto-registran en `POST /api/auth/registro`.

---

## Documentacion

| Documento | Contenido |
|-----------|-----------|
| [docs/endpoints-propuestos.md](docs/endpoints-propuestos.md) | Contrato API completo (bodies, respuestas, codigos) |
| [docs/funcionalidades.md](docs/funcionalidades.md) | Lo implementado en el backend hoy |
| [docs/dominio.md](docs/dominio.md) | Modelo de dominio y reglas de negocio |
| [docs/Acuerdos contratos.md](docs/Acuerdos contratos.md) | Acuerdos backend-frontend |
| [docs/EnunciadoOriginal.md](docs/EnunciadoOriginal.md) | Enunciado del taller |
| [docs/diagramas/ER.md](docs/diagramas/ER.md) | Diagrama entidad-relacion |
| [docs/diagramas/diagrama-clases.md](docs/diagramas/diagrama-clases.md) | Diagrama de clases del dominio |
| [docs/reporte.tex](docs/reporte.tex) | Documento de resultados e interpretacion (LaTeX) |
| [postman/GFC-Backend.postman_collection.json](postman/GFC-Backend.postman_collection.json) | Coleccion Postman ejecutable del API |

Verificacion de punta a punta con `newman run postman/GFC-Backend.postman_collection.json` contra el backend corriendo.

---

## Metricas de calidad

### Diagrama Kivati - Source Monitor

Cada tag `v*` genera un diagrama Kivati con las metricas del codigo fuente:

| Metrica | Rango aceptable | Descripcion |
|---------|----------------|-------------|
| Lineas de Codigo (M0) | < 5000 | Tamano total del proyecto |
| Sentencias (M1) | < 2000 | Sentencias ejecutables |
| Metodos por Clase (M6) | 3 - 8 | Complejidad de clases |
| Sentencias por Metodo (M7) | 3 - 8 | Tamano de metodos |
| Complejidad Ciclomatica Max (M10) | 1 - 10 | Riesgo de errores |
| Profundidad Promedio de Bloques (M13) | 1 - 2 | Anidamiento del codigo |

```bash
git tag -a v1.0.0 -m "Version 1.0"
git push origin v1.0.0
```

### SonarQube / SonarCloud

Cada tag tambien lanza un analisis de calidad com SonarCloud que **solo reporta, no bloquea**: el Quality Gate no frena el pipeline. Metricas: ratings A-E de Mantenibilidad/Fiabilidad/Seguridad (ISO 25010), complejidad ciclomatica, cobertura, duplicacion, deuda tecnica y code smells.

**Configuracion previa (una sola vez):** crear proyecto en [SonarCloud](https://sonarcloud.io) y configurar en el repo (Settings > Secrets and variables):

1. `SONAR_TOKEN` (secreto): token de SonarCloud
2. `SONAR_PROJECT_KEY` (variable): project key del proyecto
3. `SONAR_ORGANIZATION` (variable): organizacion de SonarCloud

---

## Entregables

1. Codigo fuente de la aplicacion (Java + Astro)
2. Diagrama de clases de la aplicacion (docs/diagramas/diagrama-clases.md)
3. Diagrama de casos de uso de la aplicacion
4. Documento con resultados Source Monitor + SonarQube e interpretacion (docs/reporte.tex)

**Fecha de entrega:** 8 de septiembre, 16:00

---

> **Flujo:** rama `feature/*` o `fix/*` → merge a `main` vía PR (`front` para el frontend).
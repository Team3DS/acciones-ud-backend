<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:7aa2f7,100:bb9af7&height=200&section=header&text=Contributing&fontSize=45&fontAlignY=35&desc=Backend%20%C2%B7%20Acciones%20UD&descAlignY=57&descSize=16" width="100%" alt="Contributing"/>

[![Java 21](https://img.shields.io/badge/Java_21-e0af68?style=for-the-badge)](https://dev.java)
[![Spring Boot 3.3](https://img.shields.io/badge/Spring_Boot_3.3-73daca?style=for-the-badge)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-7dcfff?style=for-the-badge)](https://maven.apache.org)
[![Docker](https://img.shields.io/badge/Docker-7dcfff?style=for-the-badge)](https://docker.com)
[![License](https://img.shields.io/badge/License-Private-bb9af7?style=for-the-badge)](LICENSE)

</div>

> ### `Objetivo`

Establecer las reglas de contribucion al backend de Acciones UD: 8 microservicios Spring Boot, cada uno dueño de su base de datos, comunicados por REST y eventos asincronos por RabbitMQ.

> ### `Los 8 microservicios`

| Servicio | Puerto | Base de datos | Dominio |
|----------|--------|---------------|---------|
| M1 Identidad y Accesos | 8081 | identidad_db | Usuarios, roles, MFA, tokens |
| M2 Cuentas y Fondos | 8082 | fondos_db | Saldo, reservas, movimientos |
| M3 Catalogo de Mercados | 8083 | catalogo_db | Instrumentos, mercados, simbolos |
| M4 Datos de Mercado | 8084 | mercado_db | Cotizaciones, cache Redis |
| M5 Ordenes y Ejecucion | 8085 | ordenes_db | Ciclo de vida orden, coordinacion |
| M6 Portafolio | 8086 | portafolio_db | Posiciones, reservas de acciones |
| M8 Notificaciones y Alertas | 8088 | avisos_db | Correos, in-app, alertas |
| M10 Suscripciones y Pagos | 8090 | pagos_db | Planes, Stripe test, premium |

Gateway (puerto 8080): punto de entrada unico /api/v1/** con enrutamiento por prefijo.

> ### `Reglas de arquitectura`

1. Cada servicio es dueno de su base de datos. Nada de FK entre servicios, nada de SELECT cruzado.
2. Comunicacion solo por contratos: REST sincronico via Gateway o eventos async por RabbitMQ.
3. M5 coordina la compra/venta: reserva M2 (fondos) y M6 (posiciones), envia a Alpaca, confirma/libera reservas, actualiza M6, publica evento para M8.
4. Transacciones locales por servicio + saga/outbox para consistencia eventual.
5. Idempotencia en comandos con clave idempotencyKey.
6. Dinero con BigDecimal scale 2 HALF_UP (MonetaryUtil en acciones-common).
7. Sin comentarios en codigo salvo que se pidan explicitamente.
8. Sin emojis en codigo ni docs.

> ### `Flujo de trabajo (Git + GitHub)`

1. Rama: tipo/descripcion-kebab (feat/, fix/, chore/, docs/).
2. Commit: tipo(scope): descripcion en espanol.
3. Un issue = un MR.
4. Flujo: feature/* -> MR -> dev -> MR -> main (produccion).
5. Descripcion del MR: Qué / Por qué / Cómo probar / Checklist.
6. CI verde obligatorio antes de merge.
7. Merge commit siempre. Nunca squash, rebase, force-push, --amend sobre commits pusheados.
8. Nunca commitear secretos ni archivos .env. Revisar git status/git diff antes de stagear.

> ### `Etiquetas y milestones (tokyonight)`

| Etiqueta | Color | Uso |
|----------|-------|-----|
| feature | #7aa2f7 | Nueva funcionalidad |
| fix | #f7768e | Correccion de bug |
| docs | #7dcfff | Documentacion |
| a11y | #9ece6a | Accesibilidad |
| perf | #e0af68 | Rendimiento |
| design | #bb9af7 | Diseno |
| content | #f7768e | Contenido |
| infra | #e0af68 | Infraestructura |
| dependencies | #f38ba8 | Dependencias |
| good first issue | #73daca | Para nuevos integrantes |
| blocked | #565f89 | Bloqueado |

| Milestone | Fecha limite |
|-----------|--------------|
| Sprint 1 - Acceso y Catalogo | 2026-11-03 |
| Sprint 2 - Operacion Financiera Core | 2026-11-17 |
| Sprint 3 - Robustez y Reportes | 2026-11-24 |
| Sprint 4 - Entrega Final | 2026-11-24 |

> ### `Puesta en marcha local`

1. Prerrequisitos: Java 21, Maven 3.9+, Docker + Docker Compose.
2. cd backend
3. cp .env.example .env y editar variables (ALPACA_API_KEY, RESEND_API_KEY, STRIPE_API_KEY).
4. docker compose up -d
5. curl http://localhost:8080/actuator/health (y 8081-8090 para cada servicio).

> ### `Desarrollo paralelo`

| Frente | Servicios | Dia 1 | Integracion real necesita |
|--------|-----------|-------|--------------------------|
| Acceso y comunicacion | M1 + M8 | Contratos, usuarios prueba, simuladores | Envio correo real (M8) |
| Consulta de mercado | M3 + M4 | Catalogo 10 simbolos, adaptador Alpaca | Credenciales Alpaca |
| Operacion financiera (ruta critica) | M2 + M5 + M6 | Reservas, movimientos, estados con mocks | Contratos M2/M6 estables, Alpaca paper |
| Frontend y plataforma | Gateway, Docker, CI, pruebas | Rutas, healthchecks, pipelines | Servicios arriba |

> ### `Definicion de Hecho por servicio`

1. Contratos documentados y aprobados (OpenAPI/AsyncAPI).
2. Migraciones Flyway/Liquibase versionadas.
3. Pruebas unitarias >= 70% cobertura (JaCoCo).
4. Pruebas de integración pasando (Testcontainers).
5. Imagen Docker construible (docker build).
6. Endpoints /actuator/health funcionando.
7. Logs estructurados JSON + correlation-id.
8. Documentacion OpenAPI (/v3/api-docs).
9. Desplegado en dev y humo pasando.

> ### `Contratos minimos`

M2 <-> M5 (Fondos <-> Ordenes):
POST /api/v1/funds/reservations -> 201 { reservationId }
POST /api/v1/funds/reservations/{id}/confirm -> 200
POST /api/v1/funds/reservations/{id}/release -> 200
GET /api/v1/accounts/{userId}/balance -> 200 { available, reserved, total }

M6 <-> M5 (Portafolio <-> Ordenes):
POST /api/v1/positions/reservations -> 201 { reservationId }
POST /api/v1/positions/reservations/{id}/release -> 200
POST /api/v1/positions/updates -> 200
GET /api/v1/portfolio/{userId} -> 200 { positions[], totalValue }

Eventos async (RabbitMQ):
M1 -> M8: UsuarioRegistrado { userId, email, tipo }
M5 -> M8: OrdenCreada | OrdenEjecutada | OrdenCancelada | OrdenRechazada
M10 -> M1/M8: SuscripcionActivada | SuscripcionCancelada | PagoFallido

> ### `Contacto`

Líder backend / Procesos y Calidad: Juan Carlos Quintero Rubiano.
Issues: GitHub Projects (Kanban Soltech: To Do -> Doing -> Blocked).

---

<div align="center">

**Soltech** · Universidad Distrital Francisco José de Caldas · 2026

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:7aa2f7,100:bb9af7&height=120&section=footer" width="100%" alt=""/>

</div>
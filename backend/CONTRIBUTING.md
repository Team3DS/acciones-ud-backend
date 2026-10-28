# CONTRIBUTING.md

> Guía de contribución para **Acciones UD Backend** — 8 microservicios Spring Boot.

## 1. Arquitectura y límites de servicio

| Servicio | Puerto | Base de datos | Responsabilidad (dueño de datos) |
|----------|--------|---------------|----------------------------------|
| **M1 Identidad** | 8081 | `identidad_db` | Usuarios, roles, MFA, sesiones, tokens |
| **M2 Cuentas y Fondos** | 8082 | `fondos_db` | Saldo contable, disponible, reservas, movimientos |
| **M3 Catálogo** | 8083 | `catalogo_db` | Instrumentos, mercados, símbolos habilitados |
| **M4 Datos de Mercado** | 8084 | `mercado_db` + Redis | Cotizaciones, feed IEX/Alpaca, caché, horarios |
| **M5 Órdenes** | 8085 | `ordenes_db` | Ciclo de vida orden, coordinación M2/M6, Alpaca |
| **M6 Portafolio** | 8086 | `portafolio_db` | Posiciones, disponibles/reservadas, valoración |
| **M8 Notificaciones** | 8088 | `avisos_db` | Correos, in-app, alertas, Resend |
| **M10 Suscripciones** | 8090 | `pagos_db` | Planes, Stripe test, webhooks, estado premium |

**Gateway** (puerto 8080) → enruta `/api/v1/**` a cada servicio.

### Reglas de oro
- **Cada servicio es dueño de su base**: nada de FK entre servicios, nada de `SELECT` cruzado.
- **Comunicación**: solo por contratos (REST sincrónico vía Gateway, o eventos async por RabbitMQ).
- **M5 coordina**: solicita reserva a M2 (fondos) y M6 (posiciones); no toca sus tablas.
- **Transacciones locales**: cada servicio gestiona la suya; coordinación con **saga/outbox**.
- **Idempotencia**: claves en comandos (crear orden, reservar, etc.).
- **Moneda**: `BigDecimal` scale 2, `HALF_UP` (`MonetaryUtil` en `acciones-common`).

## 2. Estructura del repo (multi-módulo Maven)

```
backend/
├── pom.xml                          # Parent POM (gestiona 10 módulos)
├── acciones-common/                 # Shared kernel (DTOs, events, exceptions, util)
├── gateway/                         # Spring Cloud Gateway
├── m1-identidad/
├── m2-cuentas-fondos/
├── m3-catalogo-mercados/
├── m4-datos-mercado/
├── m5-ordenes-ejecucion/
├── m6-portafolio/
├── m8-notificaciones/
├── m10-suscripciones-pagos/
├── docker-compose.yml               # Levanta TODO: 8 svcs + gateway + PG + Redis + RMQ
├── init-multi-db.sh                 # Crea 8 BDs en Postgres
└── CONTRIBUTING.md                  # Este archivo
```

Cada microservicio sigue **arquitectura hexagonal** interna:
```
src/main/java/co/edu/udistrital/acciones/<servicio>/
├── domain/           # Entidades, eventos de dominio, reglas puras (sin deps externas)
├── application/      # Casos de uso, handlers, puertos (interfaces)
├── infrastructure/
│   ├── adapter/
│   │   ├── in/       # REST controllers, messaging consumers
│   │   └── out/      # JPA repositories, Redis, RabbitMQ publishers, clientes HTTP
│   └── config/       # Beans, security, OpenAPI
└── Application.java  # @SpringBootApplication
```

## 3. Flujo de trabajo (Git + GitHub)

### Ramas
```
tipo/descripcion-kebab
feat/m1-registro-mfa
fix/m5-liberacion-reserva-fondos
docs/readme-puertos
chore/deps-jackson
```

### Commits
```
tipo(scope): descripción en español
feat(m1): registro con verificación de correo y MFA
fix(m5): liberación de reserva en cancelación
chore(gateway): actualizar rutas de m8
```

### Pull Requests
- **Un issue = un PR** (pequeños, revisables).
- **Flujo**: `feature/*` → **PR → `dev`** → **PR → `main`** (producción = Netlify/Railway).
- **Descripción PR obligatoria**: Qué / Por qué / Cómo probar / Checklist.
- **Labels** (tokyonight): `feature` #7aa2f7, `fix` #f7768e, `docs` #7dcfff, `a11y` #9ece6a, `perf` #e0af68, `design` #bb9af7, `content` #f7768e, `infra` #e0af68, `dependencies` #f38ba8, `good first issue` #73daca, `blocked` #565f89.
- **Milestones**: `Sprint 1 - Acceso y Catálogo` (2026-11-03), `Sprint 2 - Operación Financiera Core` (2026-11-17), `Sprint 3 - Robustez y Reportes` (2026-11-24), `Sprint 4 - Entrega Final` (2026-11-24).
- **CI debe pasar** antes de merge (GitHub bloquea si checks corren → esperar `success`).
- **Nunca**: squash, rebase, force-push, `--amend` sobre commits pusheados. **Merge commit** siempre.

## 4. Puesta en marcha local

```bash
# 1. Requisitos: Java 21, Maven, Docker, Docker Compose
# 2. Levantar infraestructura + todos los servicios
docker compose up -d

# 3. Verificar salud
curl http://localhost:8080/actuator/health   # Gateway
curl http://localhost:8081/actuator/health   # M1
# ... 8082-8090 para los demás
```

### Variables de entorno (.env local)
```bash
DB_USER=acciones_ud
DB_PASSWORD=acciones_ud
RABBITMQ_USER=acciones_ud
RABBITMQ_PASSWORD=acciones_ud
ALPACA_API_KEY=tu_key_paper
ALPACA_API_SECRET=tu_secret_paper
ALPACA_BASE_URL=https://paper-api.alpaca.markets
RESEND_API_KEY=re_xxx
STRIPE_API_KEY=sk_test_xxx
STRIPE_WEBHOOK_SECRET=whsec_xxx
```

## 5. Desarrollo paralelo — qué se puede hacer a la vez

| Frente | Servicios | Puede empezar día 1 | Necesita para integración real |
|--------|-----------|---------------------|--------------------------------|
| **Acceso y comunicación** | M1 + M8 (mínimo) | ✅ Contratos, usuarios de prueba, simuladores | M8 envío real de correo |
| **Consulta de mercado** | M3 + M4 | ✅ Catálogo inicial, adaptador Alpaca, Redis | Símbolos de M3, credenciales Alpaca |
| **Operación financiera (ruta crítica)** | M2 + M5 + M6 | ✅ Reservas, movimientos, estados con simuladores | Contratos M2/M6 estables, Alpaca paper |
| **Frontend y plataforma** | Gateway, Docker, CI, pruebas | ✅ Rutas, healthchecks, pipelines | Servicios arriba |

**Regla**: programar contra **contratos y simuladores (WireMock/Testcontainers)** desde el día 1. No esperar a que otro servicio termine.

### Asignación sugerida (5 personas)
| Integrante | Foco inicial |
|------------|--------------|
| Juan Carlos (Líder backend) | M5 coordinación + prueba integrada compra/venta |
| Jamyson (Backend/QA) | M1 + M8 mínimo; apoyo ejecución/mercado |
| Sebastián (DB) | Separación de BDs, migraciones M2/M6, contratos |
| Andrés (Frontend) | Catálogo, cotizaciones, formulario/seguimiento órdenes |
| Paula (Frontend/QA) | Acceso, portafolio, avisos; evidencias y docs |

> Si Sebastián no implementa backend, **quedan 2 personas para 8 servicios**. Redistribuir o recortar alcance con el cliente (profesor), no cargar todo a uno.

## 6. Contratos mínimos (definir antes de codificar)

### M2 ↔ M5 (Fondos ↔ Órdenes)
```
POST   /api/v1/funds/reservations        { orderId, userId, amount, idempotencyKey } → 201 { reservationId }
POST   /api/v1/funds/reservations/{id}/confirm   { executionId, amount } → 200
POST   /api/v1/funds/reservations/{id}/release   { reason } → 200
GET    /api/v1/accounts/{userId}/balance         → 200 { available, reserved, total }
```

### M6 ↔ M5 (Portafolio ↔ Órdenes)
```
POST   /api/v1/positions/reservations   { orderId, userId, symbol, quantity, idempotencyKey } → 201 { reservationId }
POST   /api/v1/positions/reservations/{id}/release { reason } → 200
POST   /api/v1/positions/updates        { execution } → 200
GET    /api/v1/portfolio/{userId}       → 200 { positions[], totalValue }
```

### M1 → M8 (evento)
```
UsuarioRegistrado { userId, email, tipo: "BIENVENIDA" | "MFA" }
```

### M5 → M8 (evento)
```
OrdenCreada | OrdenEjecutada | OrdenCancelada | OrdenRechazada
```

### M10 → M1/M8 (evento)
```
SuscripcionActivada | SuscripcionCancelada | PagoFallido
```

> Documentar contratos en `docs/contracts/` (OpenAPI/AsyncAPI) y versionar.

## 7. Pruebas y calidad

- **Unitarias**: JUnit 5 + Mockito (dominio puro, sin BD). Meta: ≥ 70% cobertura en lógica crítica (JaCoCo).
- **Integración**: Testcontainers (Postgres, RabbitMQ, Redis reales) + WireMock (Alpaca, Stripe, Resend).
- **Contrato**: Pact o Spring Cloud Contract entre servicios.
- **E2E**: Playwright (frontend) + k6 (carga: 1500 VU, p95 ≤ 2s, confirmación ≤ 5s).
- **Seguridad**: OWASP ZAP en entorno público.
- **Pipeline**: GitHub Actions → unitarias + integración + static analysis (SonarQube) en cada PR.

## 8. Definición de Hecho (DoD) por servicio

- [ ] Contratos documentados y aprobados.
- [ ] Migraciones Flyway/Liquibase versionadas.
- [ ] Pruebas unitarias ≥ 70% (lógica crítica).
- [ ] Pruebas de integración pasando (Testcontainers).
- [ ] Imagen Docker construible (`docker build`).
- [ ] Health/readiness endpoints (`/actuator/health`).
- [ ] Logs estructurados (JSON) + correlation-id.
- [ ] Métricas Prometheus expuestas.
- [ ] Documentación OpenAPI (`/v3/api-docs`).
- [ ] Desplegado en `dev` y humo pasando.

## 9. Convenciones de código

- **Java 21**, `record` para DTOs inmutables, `sealed` para jerarquías cerradas.
- **Sin comentarios** en código (salvo `@param`/`@return` en APIs públicas).
- **Lombok** `@Data`/`@Builder`/`@RequiredArgsConstructor` permitido.
- **Nada de `System.out.println`** → SLF4J.
- **Nada de `Float`/`Double` para dinero** → `BigDecimal` + `MonetaryUtil`.
- **Fechas**: `Instant` (UTC), ISO-8601 en JSON.
- **Excepciones**: usar las de `acciones-common` (`BusinessRuleViolationException`, `ResourceNotFoundException`, `ConflictException`, `IdempotencyException`).

## 10. Seguridad

- **HTTPS** en Gateway (terminación TLS).
- **Tokens validados también en servicios** (no solo Gateway).
- **RBAC**: usuario no accede a órdenes/portafolio ajenos.
- **Secretos**: nunca en repo. `.env` local, variables de entorno en CI/CD.
- **Bases/Redis/RabbitMQ** sin exposición pública.
- **Rate limiting** en login, MFA, creación de órdenes.

## 11. Cómo añadir un endpoint nuevo

1. Definir contrato (OpenAPI) en `docs/contracts/<servicio>.yaml`.
2. Crear DTO en `acciones-common` si es compartido, o en `application/dto` del servicio.
3. Puerto de entrada (`application/port/in/...`).
4. Caso de uso (`application/usecase/...`).
5. Adaptador REST (`infrastructure/adapter/in/rest/...`).
6. Test unitario del caso de uso.
7. Test de integración del endpoint.
8. Actualizar `CHANGELOG.md` del servicio.

## 12. Contacto

- **Líder backend / Procesos y Calidad**: Juan Carlos Quintero Rubiano
- **Issues**: GitHub (Projects "Kanban Soltech": To Do → Doing → Blocked)

---

**Soltech** · Universidad Distrital Francisco José de Caldas · 2026
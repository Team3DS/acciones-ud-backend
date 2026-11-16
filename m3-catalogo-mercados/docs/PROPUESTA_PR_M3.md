## Que

Implementa M3, que en `main` solo contenía su estructura inicial. Incluye consultas de mercados e instrumentos, filtros, paginación, validación de habilitación, edición con rol ADMINISTRADOR, idempotencia y eventos mediante outbox con confirmación RabbitMQ.

Conserva las diez acciones y sus identificadores. V3 amplía el modelo para ACCION, ETF, OPCION, FUTURO, BONO y CRYPTO, y añade lotes, ticks, estados, fechas y metadatos de mercado. La ampliación fue solicitada por Jamyson para incorporar el alcance de la wiki. El detalle y las decisiones de compatibilidad están en el ADR M3 001.

Incluye la corrección de Docker API de M3 a 1.44, la actualización de checkout/setup-java/cache a v5 con Node.js 24 y el entorno Ubuntu 24.04. La rama de origen de este PR pertenece a `Team3DS/acciones-ud-backend` y reemplaza el PR #17, que se abrió desde el fork.

## Por que

M4 y M5 necesitan un catálogo verificable y contratos reales. La wiki exige JWT, trazabilidad y edición administrativa; la implementación añade esos controles conservando los contratos y datos existentes.

Solicito revisión de la ampliación respecto del SAD, la compatibilidad del modelo y la integración con M1/M4/M5. M1 debe incluir el claim de roles para habilitar la administración real. Registrar nuevos tipos en M3 no implementa su ejecución en el proveedor ni en M5.

## Como probar

Desde la raíz: `mvn -B -pl m3-catalogo-mercados -am clean verify` para unitarias. Con Docker: `mvn -B -pl m3-catalogo-mercados -am -Pintegration,quality clean verify -Dm3.test.rabbit=true -Dm3.excluded-groups=` para PostgreSQL, RabbitMQ y calidad. En PowerShell, entrecomillar `-Pintegration,quality`.

La validación local del 16 de noviembre pasó 36 unitarias y 44 integraciones PostgreSQL por JDK en Java 21 y 25, con cero fallos y errores, Checkstyle, SpotBugs y JaCoCo correctos. También se verificó el cambio de la ruta HTTP de Testcontainers de `/v1.32/info` a `/v1.44/info` y ambos workflows pasaron actionlint. La evidencia está en `m3-catalogo-mercados/docs/VALIDACION.md`. El workflow de M3 activa además la prueba de RabbitMQ real y Docker Build; sus resultados deben revisarse en los checks del nuevo PR.

## Checklist

- [x] Código M3 compilado y empaquetado localmente.
- [x] Unitarias y pruebas PostgreSQL, incluidas idempotencia concurrente y outbox.
- [x] Cobertura mínima del 70 % en lógica crítica.
- [x] Checkstyle y SpotBugs ejecutados con el perfil documentado.
- [x] JWT, autorización ADMINISTRADOR y errores con correlación.
- [x] Migraciones y contratos OpenAPI/eventos incluidos.
- [x] Sin credenciales reales ni archivos `.env` en la entrega.
- [x] Sin comentarios nuevos ni emojis en el código propio de M3.
- [ ] Workflow completo con RabbitMQ real y Docker Build en verde.
- [ ] Resultado remoto de SonarCloud revisado.
- [ ] Integración de roles de M1 y consumidores M4/M5 verificada.
- [ ] Ampliación y ADR revisados por el equipo; SAD actualizado donde corresponda.
- [ ] Aprobación del revisor recibida.

Este PR debe permanecer abierto, con revisión solicitada y sin auto-merge, hasta resolver los puntos pendientes. No solicita una fusión inmediata.

# Validación del entregable M3

Fecha: 13 de noviembre de 2026. Rama local `feat/m3-catalogo-mercados`, base `ea53c0292e3f846517567eb903169c8e14fd85a0`. Alcance ampliado por solicitud de Jamyson: catálogo, administración, tipos, idempotencia y eventos.

## Resultados ejecutados

| Comprobación | Resultado |
| --- | --- |
| Compilación y JAR Java 21 | Correctos |
| Unitarias | 36 correctas |
| Integración con PostgreSQL 16.4 | 44 correctas |
| Total local | 80 ejecutadas, cero fallos o errores |
| Checkstyle | Cero infracciones con la configuración incluida |
| SpotBugs | Correcto con esfuerzo Max y umbral High |
| JaCoCo líneas | 655/672 (97.5 %) |
| JaCoCo ramas | 198/232 (85.3 %) |
| Umbral crítico de consultas, administración y elegibilidad | Al menos 70 %, cumplido |
| Compatibilidad V3 | Preserva 17 columnas originales, diez restricciones y los datos e IDs de las diez acciones; amplía intencionalmente el ticker a 20 caracteres |
| Adopción del esquema original | Baseline 1, V2 y V3 correctos; no duplica las acciones |
| HTTP real contra el JAR | Doce solicitudes correctas, incluidas 401, 403, administración e idempotencia |
| OpenAPI exportado | Once rutas, con autenticación Bearer |

La cobertura incluye dominio, aplicación, persistencia, controladores, seguridad y mensajería; excluye la clase de arranque. No acredita ausencia de defectos ni aceptación del producto completo.

## Casos relevantes

Se probaron búsquedas, filtros de wiki y anteriores, paginación, SQL parametrizado, símbolos ambiguos/desconocidos, JWT inválido o vencido, correlación, rol de administrador, creación de los seis tipos, cambios de estado, lotes y ticks positivos, conflictos de idempotencia y reintentos concurrentes. Dos solicitudes simultáneas producen un instrumento y un evento.

Las pruebas PostgreSQL comprueban el outbox ante fallo y posterior recuperación del publicador. Las unitarias del transporte comprueban ack, nack, devolución por falta de ruta y fallo de confirmación. Hay una prueba adicional de RabbitMQ real en el grupo `broker`, activada por el workflow; no formó parte del recorrido local sin Docker.

## Alcance de la evidencia

Se ejecutó `mvn -B -pl m3-catalogo-mercados -am '-Pintegration,quality' clean verify` con una base PostgreSQL exclusiva indicada mediante propiedades de pruebas. El reporte estructurado está en `validation-results.json`. Los reportes completos permanecen en `target/` y no se suben.

`schema-parity.json` documenta los cambios deliberados respecto del SQL original; la igualdad exacta pertenecía a V1/V2, no al modelo ampliado V3. `baseline-smoke.json` registra la adopción y `http-smoke.json` las comprobaciones HTTP. Estas últimas usan JWT de fixtures y desactivan el relay y el healthcheck Rabbit para aislar la API; no se presentan como una integración real con el broker.

## Pendientes antes de una fusión

Ejecutar en CI la integración con RabbitMQ real y Docker Build; revisar SonarCloud remoto; conectar la emisión del claim de roles en M1; validar los consumidores M4/M5 y el Gateway desplegado; aplicar permisos con psql por un administrador y ratificar la evolución del SAD/ADR por el revisor. M3 no modifica el código de M1, M2, M4, M5 u otros servicios; fuera del módulo solo hay ajustes mínimos de rutas, entorno, workflow y nota de documentación.

El PR debe permanecer abierto y sin auto-merge hasta recibir aprobación y cumplir los checks exigidos. No se ha publicado un PR ni se ha recibido una aprobación remota durante esta sesión.

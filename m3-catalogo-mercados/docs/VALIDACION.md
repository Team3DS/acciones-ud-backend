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

El PR debe permanecer abierto y sin auto-merge hasta recibir aprobación y cumplir los checks exigidos. El PR [#17](https://github.com/Team3DS/acciones-ud-backend/pull/17) se abrió desde el fork `JamysonRolon/acciones-ud-backend`; trasladar el trabajo a una rama de `Team3DS` requiere un PR nuevo porque GitHub no permite cambiar el repositorio de origen de un PR existente.

## Corrección de CI del 16 de noviembre de 2026

Se revisó la ejecución [43729803089](https://github.com/Team3DS/acciones-ud-backend/actions/runs/43729803089), correspondiente al commit `1010d1b95f90a9bab2fb3a5c7af4d0702e7c06c9`. Las unitarias pasaban; las 45 pruebas de integración fallaban al iniciar el contexto porque Testcontainers 1.20.3 enviaba peticiones con Docker API 1.32 y el daemon exigía como mínimo 1.40. Los avisos de Node.js y setup-java eran independientes de ese fallo.

Se añadió `src/test/resources/docker-java.properties` con `api.version=1.44`, igual que en M1 y M2. Es configuración exclusiva de las pruebas. Las acciones checkout, setup-java y cache se actualizaron a v5, fijadas por SHA y con runtime Node.js 24 verificado. Los workflows usan Ubuntu 24.04 y M3 también se ejecuta al subir `feat/m3-catalogo-mercados`.

La comprobación del transporte HTTP de Testcontainers reprodujo `/v1.32/info` sin el recurso y `/v1.44/info` con el recurso copiado a `target/test-classes`, tanto en Java 21 como en Java 25. Esta comprobación usa un servidor HTTP de prueba; no representa una ejecución de contenedores.

Los dos workflows pasaron actionlint 1.7.12. La verificación local usa bases PostgreSQL 16.4 separadas por JDK. Este equipo no tiene Docker: el caso de RabbitMQ real y la construcción de la imagen siguen pendientes de la ejecución remota después del push. Se conservan activados en CI; no se omiten ni se convierten sus fallos en éxitos.

| JDK local | Unitarias | Integración PostgreSQL | Fallos / errores | Calidad |
| --- | --- | --- | --- | --- |
| Temurin 21.0.13+9 | 36 | 44 | 0 / 0 | Checkstyle, SpotBugs y JaCoCo correctos |
| Temurin 25.0.5+8 | 36 | 44 | 0 / 0 | Checkstyle, SpotBugs y JaCoCo correctos |

Ambos recorridos ejecutaron `mvn -B -pl m3-catalogo-mercados -am '-Pintegration,quality' clean verify` con `m3.test.jdbc-url` apuntando a su base exclusiva. El caso `broker` se excluye por defecto en este recorrido local, tal como está definido en el POM. CI activa explícitamente `-Dm3.test.rabbit=true -Dm3.excluded-groups=` para incluirlo.

Los registros de esta revisión se guardan fuera del repositorio, en `output/m3-java21-verify-2026-11-16.log`, `output/m3-java25-verify-2026-11-16.log` y `output/m3-docker-api-probe-2026-11-16.log`, en la carpeta padre del checkout.

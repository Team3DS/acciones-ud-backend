# Revisión de reglas y wiki para M3

Fecha: 13 de noviembre de 2026. Esta revisión complementa la primera entrega, que había examinado el repositorio pero no la wiki. Se corrigieron las omisiones de seguridad y contratos detectadas al ampliar la revisión.

## Fuentes revisadas

- README, CONTRIBUTING, plantilla de PR y workflows del commit base `ea53c0292e3f846517567eb903169c8e14fd85a0`.
- Inventario de las 12 páginas publicadas en la wiki, corte `a4e411879e7ebc80c30b4deb3d4bba74b6231905`; lectura dirigida de M3, arquitectura, contratos comunes, despliegue y relaciones con M1/M4/M5.
- API pública de ramas y reglas de GitHub, historial de PR y metadatos comunitarios de la organización.
- SAD v0.2 y SQL `03_catalogo_db.sql` aportados para este proyecto.

La portada enlaza guías de desarrollo, convenciones, variables, ADR y modelo global que no están presentes como páginas independientes en el repositorio de la wiki descargada. No se atribuyen reglas adicionales a esos enlaces sin contenido disponible.

## Cumplimiento y decisiones pendientes

| Regla o contrato | Estado del entregable |
| --- | --- |
| Java 21, Spring Boot 3.3.x y Cloud 2023.x | Coincide con el POM y la guía de M3; las correcciones generales del repositorio quedan fuera de este entregable |
| Arquitectura por puertos y adaptadores | Dominio sin dependencias de infraestructura; acceso a datos a través de un puerto |
| Base privada y ausencia de FK entre servicios | Tablas propias de `catalogo_db`, incluidas idempotencia y outbox; joins y FK internos |
| `/api/v1/` y rutas de M3 | Se conservan `/markets` y `/instruments` y se añade `/catalog/markets`; Gateway actualizado |
| Detalle por MIC y por código de mercado | Se admiten XNAS/XNYS y NASDAQ/NYSE |
| Elegibilidad `/instruments/{symbol}/tradable` | Alias compatible; valida habilitación, no horario o fondos, que corresponden a M4/M5 |
| `filter[campo]` y `sort=campo,asc` | Compatibles con los parámetros anteriores; filtros contradictorios producen 400 |
| Paginación desde cero, máximo 100 | Incluye contenido, número, tamaño, totales y banderas de primera/última/vacía |
| JWT Bearer | Obligatorio para el catálogo; valida firma, expiración e identidad; compatible con los tokens emitidos por M1 |
| Salud y documentación pública | Excepciones explícitas de seguridad para Actuator y Swagger/OpenAPI |
| `X-Correlation-ID` | Se valida o genera, se devuelve y se registra en MDC; también aparece como `traceId` en errores |
| Errores estructurados | ProblemDetail más `timestamp`, `status`, `error`, `message`, `path`, `traceId` y `code` |
| Logs JSON y Prometheus | Encoder JSON, identificación de M3 y contexto de correlación; endpoint de métricas |
| Idempotencia en mutaciones | Clave por usuario y operación; escritura, respuesta y evento atómicos; reintentos concurrentes probados |
| Pruebas y cobertura de lógica crítica | Ver resultados actuales en VALIDACION.md y validation-results.json |
| Sin secretos, emojis o comentarios nuevos en código | Comprobado en los archivos propios del entregable; las claves fijas de pruebas están únicamente en fixtures identificados como pruebas |
| Mensaje de commit y plantilla de PR | Texto preparado para que Jamyson lo utilice manualmente; no hay commit ni PR publicados |
| Docker, CI remoto y aprobación | Pendientes de ejecutarse o recibirse tras la subida manual |

## Contradicciones que debe resolver el revisor

La wiki de M3 está identificada como versión 0.1, arquitectura planificada y esqueleto. Propone `instrumento`, MIC como clave, edición por administrador, otros tipos de activos, horarios dentro de M3 y eventos de creación/actualización. El SAD y el SQL suministrados usan `mercado`, `sector`, `accion`, claves internas numéricas, diez acciones y horarios en M4.

Jamyson eligió expresamente incluir la edición administrativa y ampliar el modelo de la wiki. Se implementó V3 con los seis tipos, estados, lotes, ticks, fechas, metadatos de mercado, permisos de escritura, idempotencia y eventos outbox. Se mantienen los IDs y nombres físicos anteriores para compatibilidad. El ADR M3 001 declara esta evolución para su revisión por el equipo y actualización del SAD, sin presentarla como aprobación ya recibida del cliente.

La wiki dibuja adaptadores JPA; esta implementación utiliza un adaptador JDBC detrás del mismo tipo de puerto de salida para conservar el SQL. Esa diferencia se declara para revisión y no se marca como cumplimiento literal del diagrama JPA.

El orden por precio, variación y volumen de RF-19 y la interpretación de «operable hoy» necesitan composición con M4/M5. La elegibilidad local no se presenta como autorización completa para negociar.

La arquitectura general menciona SpotBugs, Checkstyle y Sonar Quality Gate. Se añadió el perfil `quality` de M3 con Checkstyle y SpotBugs (esfuerzo Max, umbral High), que también ejecuta el workflow de M3. El workflow base mantiene SonarCloud no bloqueante: el resultado remoto y su aceptación siguen pendientes y no se sustituyen por una afirmación de éxito local.

El M1 del commit base genera tokens sin el claim de roles. M3 verifica `ADMINISTRADOR` o `ROLE_ADMINISTRADOR` en tokens firmados para las ediciones. Esa integración debe conectarse en M1; M3 no omite la autorización ni genera un administrador alternativo. Los tests crean sus propios fixtures firmados y no demuestran la emisión real de roles por M1.

## Reglas configuradas en GitHub

La API pública informa `main` como protegida. Los endpoints públicos de rulesets y reglas aplicadas devuelven listas vacías; esto no demuestra que falte protección clásica. La consulta del detalle de protección devuelve HTTP 401 y requiere acceso autenticado. No se pudo confirmar públicamente el número exigido de aprobaciones, revisores o checks obligatorios.

El procedimiento preparado usa una rama, un PR hacia `main`, revisión solicitada y ausencia de merge o auto-merge. No se alteraron protecciones ni permisos. Si el equipo necesita impedir técnicamente una fusión sin aprobación, un administrador debe verificar la protección de `main` y la exigencia de revisiones; este entregable no modifica esa configuración.

## Referencias

- [Wiki de M3](https://github.com/Team3DS/acciones-ud-backend/wiki/Microservicios/M3-Catalogo-Mercados)
- [Contratos REST y eventos](https://github.com/Team3DS/acciones-ud-backend/wiki/Contratos-REST-y-Eventos)
- [Arquitectura general](https://github.com/Team3DS/acciones-ud-backend/wiki/Arquitectura-General)
- [CONTRIBUTING del commit base](https://github.com/Team3DS/acciones-ud-backend/blob/ea53c0292e3f846517567eb903169c8e14fd85a0/CONTRIBUTING.md)
- [Plantilla de PR](https://github.com/Team3DS/acciones-ud-backend/blob/ea53c0292e3f846517567eb903169c8e14fd85a0/.github/PULL_REQUEST_TEMPLATE.md)
- [Reglas públicas del repositorio](https://github.com/Team3DS/acciones-ud-backend/rules)

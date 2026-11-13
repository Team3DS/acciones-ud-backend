# M3 Catálogo de mercados

Entregable de Jamyson para revisión del equipo. Incluye consulta del catálogo, edición administrativa, autenticación JWT, idempotencia y eventos. La ampliación respecto de la primera versión de solo lectura fue solicitada expresamente por Jamyson el 13 de noviembre de 2026 para incorporar el alcance de la wiki.

La rama local es `feat/m3-catalogo-mercados`, creada desde `main` en `ea53c0292e3f846517567eb903169c8e14fd85a0`. No se ha hecho commit, push, PR ni despliegue. La subida será manual y el PR deberá permanecer abierto hasta recibir revisión y aprobación.

## Modelo y compatibilidad

`V1` conserva las tres tablas del SQL original y `V2` las dos bolsas, seis sectores y diez acciones iniciales. `V3` agrega país y horario de referencia del mercado, tipos de instrumento, lotes, ticks, estados, fechas, idempotencia y outbox. Amplía el ticker de 10 a 20 caracteres. No reemplaza claves ni modifica los identificadores existentes.

El catálogo admite `ACCION`, `ETF`, `OPCION`, `FUTURO`, `BONO` y `CRYPTO`. Los estados son `ACTIVO`, `SUSPENDIDO` y `RETIRADO`. Los importes de lote y tick son decimales positivos de hasta ocho posiciones. La moneda sigue restringida a USD para conservar el contrato con el resto del sistema; añadir monedas requiere otro cambio coordinado.

La tabla histórica `accion` conserva su nombre físico y contiene ahora los metadatos de los instrumentos. Esta decisión evita romper el SQL y las referencias existentes. La wiki usa el nombre conceptual `instrumento` y dibuja adaptadores JPA; aquí se emplea JDBC detrás de un puerto de salida. La justificación está en `docs/ADR-M3-001.md` y queda a revisión del equipo.

Los horarios dentro de M3 son metadatos de referencia. M4 conserva la responsabilidad sobre calendario operativo, feriados y sesión efectiva. `tradable` valida únicamente habilitación del catálogo; no autoriza por sí mismo una orden ni garantiza que el proveedor soporte un tipo de activo. M5 debe comprobar horario, proveedor, usuario, fondos, tenencia y reglas aplicables antes de operar.

## Organización

- `domain`: registros inmutables, tipos, estados, resultados y sobre de eventos.
- `application`: consultas y comandos, reglas de elegibilidad y puertos.
- `infrastructure/adapter/in/rest`: API, validación y ProblemDetail.
- `infrastructure/adapter/out/persistence`: SQL parametrizado, bloqueo de filas, registro de idempotencia y eventos en la misma transacción.
- `infrastructure/security`: validación de tokens y roles.
- `infrastructure/adapter/in/scheduling` y `adapter/out/messaging`: relay de outbox y publicación RabbitMQ confirmada.

Todos los joins y las FK permanecen dentro de `catalogo_db`. Ningún servicio externo debe leer directamente sus tablas.

## API y permisos

Puerto: `8083`. El Gateway conserva las rutas anteriores y añade `/api/v1/catalog/**`.

| Método y ruta | Permiso | Función |
| --- | --- | --- |
| GET `/api/v1/catalog/markets` | JWT | Mercados habilitados; `enabled=false` lista los deshabilitados |
| GET `/api/v1/catalog/markets/{code}` | JWT | Detalle por código o MIC, por ejemplo NASDAQ o XNAS |
| GET `/api/v1/instruments/sectors` | JWT | Sectores |
| GET `/api/v1/instruments` | JWT | Búsqueda, filtros y paginación |
| GET `/api/v1/instruments/{symbol}` | JWT | Detalle; `market` desambigua el ticker |
| GET `/api/v1/instruments/id/{id}` | JWT | Detalle por identificador |
| GET `/api/v1/instruments/{symbol}/tradable` | JWT | Habilitación de catálogo |
| POST `/api/v1/instruments/validate` | JWT | Validación de un lote, sin escritura |
| POST `/api/v1/catalog/markets` | ADMINISTRADOR | Crear mercado |
| PATCH `/api/v1/catalog/markets/{code}` | ADMINISTRADOR | Cambiar nombre, país, habilitación y horario de referencia |
| POST `/api/v1/instruments` | ADMINISTRADOR | Crear instrumento |
| PATCH `/api/v1/instruments/{symbol}` | ADMINISTRADOR | Cambiar nombre, estado, lote y tick; admite `market` |

`/api/v1/markets` es un alias de las rutas de mercados. `/validation` es un alias de `/tradable`. Se mantienen para compatibilidad con el código existente.

Las lecturas requieren `Authorization: Bearer TOKEN`. Se verifican firma, expiración e identidad numérica positiva, utilizando la misma clave UTF-8 de M1. Las escrituras exigen el claim `roles` con `ADMINISTRADOR` o `ROLE_ADMINISTRADOR`. M3 no emite tokens ni ofrece un acceso alternativo. El M1 del commit base emite tokens sin roles: el equipo debe conectar esa emisión para las operaciones administrativas reales. Los tokens de las pruebas son fixtures y no sirven como mecanismo de acceso del producto.

Actuator y Swagger/OpenAPI permanecen públicos según las convenciones de la wiki. No hay borrado físico por API: un instrumento se retira cambiando su estado.

### Consulta y paginación

Se admiten `query` por ticker/empresa, `symbol` exacto, `market`, `sector`, `type`, `enabled`, `page`, `size`, `sort` y `direction`. También se aceptan `filter[query]`, `filter[symbol]`, `filter[market]`, `filter[sector]`, `filter[type]` y `filter[enabled]`.

`sort=symbol,asc` y `sort=companyName,desc` siguen el formato de la wiki. Son compatibles con `sort=SYMBOL&direction=asc`. Los campos permitidos son símbolo, empresa, mercado y sector. Ordenar por precio, variación o volumen requiere componer datos con M4; no se inventan esos valores en M3.

Las páginas empiezan en cero y tienen tamaño predeterminado 20, máximo 100. La respuesta incluye `content`, `page`, `number`, `size`, `totalElements`, `totalPages`, `numberOfElements`, `first`, `last` y `empty`. Un filtro sin coincidencias o una página fuera del rango devuelven contenido vacío. Los filtros contradictorios producen 400.

Una validación por lote recibe `{"symbols":["AAPL","ZZZZ"],"market":"NASDAQ"}`. Se admiten de 1 a 100 símbolos de hasta 20 caracteres. Se normalizan a mayúsculas y se eliminan duplicados conservando su orden. Los motivos son `HABILITADO`, `NO_ENCONTRADO`, `ACCION_DESHABILITADA`, `MERCADO_DESHABILITADO` y `SIMBOLO_AMBIGUO`. Un detalle ambiguo devuelve 409 y requiere mercado.

### Administración e idempotencia

Todo POST/PATCH que modifica el catálogo exige `Idempotency-Key`, de 1 a 100 letras, números, puntos, guiones o guiones bajos. La clave se delimita por usuario y operación. Repetirla con el mismo contenido devuelve el resultado original y `Idempotency-Replayed: true`; cambiar el contenido produce 409. El registro, el resultado de la operación y el evento se confirman en una única transacción. Se prueba también la concurrencia de reintentos.

Ejemplo de mercado:

```json
{"code":"TEST","mic":"XTST","name":"Mercado de prueba","timeZone":"America/Bogota","currency":"USD","country":"Colombia","tradingHours":{"open":"09:00","close":"15:00"}}
```

`code` se deriva del MIC si se omite; `timezone` se admite como alias de `timeZone`. El MIC debe tener cuatro caracteres y la zona debe ser IANA válida.

Ejemplo de instrumento:

```json
{"symbol":"TESTETF","name":"ETF de prueba","market":"NASDAQ","sector":"TECNOLOGIA","type":"ETF","lotSize":1,"tickSize":0.01}
```

Ejemplo de cambio de estado: `{"state":"SUSPENDIDO"}`. El símbolo, tipo, mercado y sector del instrumento no se cambian mediante este PATCH para conservar sus referencias históricas. Los parches vacíos o con valores inválidos devuelven 400. Duplicados y conflictos de integridad devuelven 409 sin exponer SQL.

### Eventos y observabilidad

Se registran `m3.mercado.creado`, `m3.mercado.actualizado`, `m3.instrumento.creado` y `m3.instrumento.actualizado`. El exchange durable es `acciones.ud`. El sobre contiene `eventId`, `eventType`, `occurredAt`, `aggregateId`, `correlationId` y `payload`.

El relay usa `FOR UPDATE SKIP LOCKED`, mensajes persistentes, publisher confirms y detección de mensajes sin ruta. Solo marca el evento después de confirmación; un fallo deja el evento pendiente y programa un reintento a 30 segundos. La entrega es al menos una vez: M4 y los demás consumidores deben deduplicar por `eventId` y declarar sus colas/bindings. El contrato está en `docs/contracts/EVENTOS_M3.md`.

`X-Correlation-ID` se conserva si es válido o se genera. Aparece en la respuesta, logs JSON y errores. Los errores usan ProblemDetail más `timestamp`, `error`, `message`, `path`, `traceId` y `code`. No se registran tokens ni cuerpos de solicitudes en los logs HTTP.

## Ejecución local

Requisitos: Java 21 y Maven 3.9. El entorno completo necesita PostgreSQL 16 y RabbitMQ 4. Desde la raíz del repositorio, en PowerShell:

```powershell
$env:M3_LOCAL_DB_PASSWORD = Read-Host 'Contraseña de PostgreSQL local'
$env:M3_LOCAL_RABBIT_PASSWORD = Read-Host 'Contraseña de RabbitMQ local'
docker compose -f m3-catalogo-mercados/compose.local.yml up -d
$env:DB_HOST='127.0.0.1'
$env:DB_PORT='55434'
$env:DB_NAME='catalogo_db'
$env:DB_USER='acciones_ud'
$env:DB_PASSWORD=$env:M3_LOCAL_DB_PASSWORD
$env:RABBITMQ_HOST='127.0.0.1'
$env:RABBITMQ_PORT='5674'
$env:RABBITMQ_USER='acciones_ud'
$env:RABBITMQ_PASSWORD=$env:M3_LOCAL_RABBIT_PASSWORD
$env:JWT_SECRET=Read-Host 'JWT_SECRET acordado para desarrollo, el mismo de M1'
mvn -pl m3-catalogo-mercados -am package -DskipTests
java -jar m3-catalogo-mercados/target/m3-catalogo-mercados-0.0.1-SNAPSHOT.jar
```

Para comprobar la API, obtener un token válido de M1 y establecerlo como `M3_ACCESS_TOKEN` en la sesión:

```powershell
$headers=@{Authorization="Bearer $env:M3_ACCESS_TOKEN"}
Invoke-RestMethod http://localhost:8083/actuator/health
Invoke-RestMethod http://localhost:8083/api/v1/catalog/markets -Headers $headers
Invoke-RestMethod 'http://localhost:8083/api/v1/instruments?filter[type]=ACCION&sort=symbol,asc' -Headers $headers
```

OpenAPI: `/v3/api-docs`; Swagger UI: `/swagger-ui/index.html`; métricas: `/actuator/prometheus`. El contrato exportado está en `docs/contracts/m3-openapi.json`.

## Migraciones y permisos

Sobre una base vacía, Flyway aplica V1, V2 y V3. Si existe el esquema original sin historial, respaldarlo y comprobarlo antes de usar, solo en el primer arranque, `SPRING_FLYWAY_BASELINE_ON_MIGRATE=true` y `SPRING_FLYWAY_BASELINE_VERSION=1`. Después deben retirarse esas opciones. La adopción y la ampliación se prueban sin recrear las diez acciones ni cambiar sus IDs. No reescribir V1/V2 ni volver a ejecutar el SQL antiguo sobre el esquema ampliado como sustituto de Flyway.

`db/provision/m3-permissions.psql` se aplica después de las migraciones con un administrador y `CATALOGO_APP_PASSWORD` definida. Concede a `catalogo_app` lectura del sector y lectura/escritura limitada a las tablas propias de M3, sin DDL ni DELETE. Separar sus credenciales `DB_USER`/`DB_PASSWORD` de `M3_MIGRATION_USER`/`M3_MIGRATION_PASSWORD`; estas últimas deben pertenecer a un usuario autorizado a migrar los objetos de `catalogo_owner`.

## Pruebas y calidad

Unitarias, sin PostgreSQL o Docker:

```powershell
mvn -B -pl m3-catalogo-mercados -am clean verify
```

Unitarias, PostgreSQL, Checkstyle y SpotBugs con una base exclusiva de pruebas:

```powershell
mvn -B -pl m3-catalogo-mercados -am '-Pintegration,quality' clean verify `
  '-Dm3.test.jdbc-url=jdbc:postgresql://127.0.0.1:5432/catalogo_m3_test' `
  '-Dm3.test.username=postgres' '-Dm3.test.password=CLAVE_DE_PRUEBAS'
```

Para ejecutar también la prueba real de RabbitMQ, con Docker disponible:

```powershell
mvn -B -pl m3-catalogo-mercados -am '-Pintegration,quality' clean verify `
  '-Dm3.test.rabbit=true' '-Dm3.excluded-groups='
```

El último comando usa Testcontainers para PostgreSQL y RabbitMQ. La prueba de grupo `broker` no se incluye en el recorrido local sin Docker; el workflow de M3 sí la activa. No ejecutar estas suites sobre bases con información del proyecto: usan fixtures y una prueba concurrente con limpieza de sus registros.

Se exige al menos 70 % de cobertura de líneas en consultas, administración y elegibilidad. Checkstyle comprueba convenciones básicas de producción; SpotBugs se ejecuta con esfuerzo máximo y umbral High. Los resultados actuales y límites están en `docs/VALIDACION.md` y `docs/REVISION_REGLAS_Y_WIKI.md`.

Para la subida manual y la espera de aprobación, seguir `docs/SUBIDA_Y_APROBACION.md`. No subir `target/`, herramientas, bases temporales, tokens ni archivos `.env`.

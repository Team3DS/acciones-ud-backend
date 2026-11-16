# M8 Notificaciones y Alertas

Servicio Spring Boot/Java 21, puerto 8088, base propia `avisos_db`. Implementa RF-48, RF-49, RF-53, RF-54, RF-55, RF-60 y la coordinación de RF-68. La base es el SAD revisado y `07_avisos_db.sql` del 30 de octubre; la wiki se usa como referencia de flujos, conservando los nombres, restricciones y condiciones del modelo SQL.

## Comportamiento

- Centro de notificaciones con paginación, indicador de no leídas y marcado de lectura. La identidad siempre procede del JWT firmado por M1; no se acepta un usuario de destino desde el navegador.
- Preferencias por usuario, evento y canal `APP`/`CORREO`. Sin fila se habilitan ambos canales; se aplica la preferencia vigente al procesar el evento.
- Suscripciones propias a avisos de NYSE/NASDAQ. La distribución se procesa por lotes y deduplica cada destinatario.
- Alertas `SUPERA`/`CAE_BAJO`, con umbral positivo de hasta cuatro decimales. La igualdad no dispara. Se comprueba premium con M10 y la acción con M3 antes del alta y en cada evaluación. Una cotización de más de dos minutos, futura o inválida no dispara. La cancelación o caducidad premium desactiva las alertas; un fallo de M10 impide crear o disparar alertas sin inventar un estado de suscripción.
- Procesamiento transaccional del inbox, la notificación y el correo pendiente. Un evento repetido, incluso concurrente, produce un solo efecto por usuario.
- Correos mediante la API oficial de Resend. Su fallo no bloquea al emisor financiero. La entrega usa `Idempotency-Key`, confirmación del proveedor, reintentos limitados y una ventana inferior a las 24 horas de retención de claves de Resend.
- OTP, verificación, recuperación y bienvenida de M1 se entregan por correo, fuera del centro de notificaciones. Los contenidos pendientes se cifran con AES-256-GCM; se eliminan al terminar, expirar o agotar intentos. No se registran cuerpos ni secretos temporales en logs.
- Reporte mensual con resultado bruto, comisiones separadas y resumen de activos de los servicios propietarios. Nunca se presenta como resultado neto ni se sustituyen fuentes ausentes por ceros. Existe un solo registro por usuario/periodo y se confirma el envío únicamente después de Resend.

El scheduler de reportes está implementado y permanece desactivado por defecto hasta que M1/M5/M6 publiquen los contratos de origen. Al activarlo, se ejecuta a las 07:00 de Bogotá los primeros siete días de cada mes para recuperar fuentes temporalmente caídas, sin duplicar reportes ya preparados.

## API

Todos los recursos `/api/v1/**` requieren JWT con `sub` numérico positivo, firma válida y caducidad. Los usuarios solo acceden a sus propios datos.

| Método | Ruta | Función |
| --- | --- | --- |
| GET | `/api/v1/notifications?page=0&size=20&unreadOnly=false` | Listar notificaciones visibles |
| GET | `/api/v1/notifications/unread-count` | Contar no leídas |
| PATCH | `/api/v1/notifications/{id}/read` | Marcar como leída, idempotente |
| GET / PATCH | `/api/v1/notifications/preferences` | Consultar o modificar una preferencia |
| GET | `/api/v1/notifications/markets` | Mercados suscritos |
| POST / DELETE | `/api/v1/notifications/markets/{market}` | Suscribirse o desuscribirse |
| GET / POST | `/api/v1/alerts/price` | Listar o crear alertas |
| DELETE | `/api/v1/alerts/price/{id}` | Desactivar una alerta propia |

Ejemplo de preferencia: `{"type":"EJECUCION_ORDEN","channel":"CORREO","enabled":false}`.

Ejemplo de alerta: `{"instrumentId":1,"condition":"SUPERA","targetPrice":150.25}`. POST requiere `Idempotency-Key` (1–100 caracteres alfanuméricos, punto, guion, dos puntos o guion bajo). Repetir los mismos datos devuelve la misma alerta; reutilizar la clave con otros datos devuelve 409.

Las rutas de preferencias están dentro de `/notifications` para utilizar el enrutamiento actual del Gateway. No existe un endpoint público para enviar correos arbitrarios u OTP a otros usuarios. OpenAPI: `/v3/api-docs` y `/swagger-ui/index.html`. Salud: `/actuator/health`, sin detalles internos.

## Configuración

| Variable | Uso |
| --- | --- |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | PostgreSQL; base predeterminada `avisos_db` |
| `M8_MIGRATION_USER`, `M8_MIGRATION_PASSWORD` | Credenciales de migración separadas, si se emplean roles restringidos |
| `JWT_SECRET` | Misma clave de firma configurada en M1, mínimo 32 bytes |
| `M8_MAIL_KEY` | Clave AES de 32 bytes en Base64; obligatoria y estable entre reinicios |
| `RESEND_API_KEY`, `RESEND_FROM` | Credencial y remitente autorizado del proveedor |
| `RESEND_BASE_URL` | API del proveedor, predeterminada `https://api.resend.com` |
| `M8_SERVICE_TOKEN` | Credencial para contratos entre servicios; no se usa el JWT del navegador en tareas de fondo |
| `M1_BASE_URL`, `M3_BASE_URL`, `M4_BASE_URL`, `M5_BASE_URL`, `M6_BASE_URL`, `M10_BASE_URL` | Direcciones de los servicios propietarios |
| `M8_RABBIT_ENABLED`, `M8_JOBS_ENABLED` | Consumidor y tareas de fondo; activados por defecto |
| `M8_MONTHLY_ENABLED` | Scheduler mensual; desactivado hasta integrar sus fuentes |

En Git Bash puede generarse una clave con `openssl rand -base64 32`. Guardarla una sola vez como `M8_MAIL_KEY` en el `.env` local ignorado por Git o en el gestor de secretos del despliegue; regenerarla al reiniciar impediría descifrar entregas pendientes. No incluir claves en commits, capturas o descripciones del PR.

## Base de datos

Flyway crea las seis tablas del SQL aprobado en V1. Ante una base provisionada previamente con ese SQL, establece baseline 1 y aplica V2 sin borrar datos. V2 añade columnas técnicas para deduplicación, metadatos de alertas y `entrega_correo`. No introduce FK hacia otras bases ni modifica los tipos de eventos de negocio originales.

El aprovisionamiento de roles y contraseñas del SQL original se ejecuta por un administrador con psql; no se confunde con migraciones JDBC. Flyway debe ejecutarse con un usuario que pueda modificar el esquema. Cuando existe `avisos_app`, V2 le concede SELECT/INSERT/UPDATE sobre la tabla nueva. Revisar el esquema existente antes de adoptar una base; una versión incompatible debe corregirse con una migración revisada, no deshabilitando validaciones.

## Eventos, reintentos y dependencias

Ver [CONTRATOS.md](docs/CONTRATOS.md) para el sobre JSON, las rutas RabbitMQ y los contratos HTTP. Las colas principales, de reintento y de errores son durables. El consumidor confirma el mensaje después del commit; un fallo transitorio de base se reintenta hasta tres veces mediante una cola con TTL, esperando confirmación del broker antes de confirmar el original. Un mensaje inválido o agotado llega a `m8.notificaciones.dlq` para revisión.

Los correos tienen hasta cinco intentos con espera creciente. Un 429/5xx, fallo de red o respuesta sin identificador no marca un envío exitoso. Los errores permanentes terminan como `FALLIDO`; las entregas fuera de ventana como `EXPIRADO`. Revisar esas filas y la DLQ operativamente, sin publicar sus cuerpos. No reactivar automáticamente una entrega fuera de la ventana de idempotencia del proveedor.

Los contratos internos faltantes en otros servicios se detallan expresamente. Una prueba con WireMock acredita el comportamiento del consumidor del contrato, no que el productor remoto ya lo implemente. La integración de extremo a extremo con M1/M4/M5/M6/M10 requiere esos productores, credenciales y revisión del equipo.

## Verificación

Unitarias, sin infraestructura:

```bash
mvn -B -pl m8-notificaciones -am clean verify
```

Recorrido completo con Docker disponible, sin credenciales reales de Resend:

```bash
mvn -B -pl m8-notificaciones -am -Pintegration,quality clean verify
docker build -f m8-notificaciones/Dockerfile -t acciones-ud/m8:local .
```

El perfil de integración inicia PostgreSQL y RabbitMQ mediante Testcontainers y simula HTTP con WireMock. No omite pruebas cuando falta Docker: el recorrido completo debe fallar con una causa visible. Las pruebas utilizan `docker-java.properties` con API 1.44 y colas compatibles con RabbitMQ 4.3.

Para depurar exclusivamente API/PostgreSQL con una base local desechable, se admite `-Dit.test=AvisosIT -Dm8.test.jdbc-url=jdbc:postgresql://localhost:5432/base_pruebas -Dm8.test.username=postgres`. Ese recorrido no acredita RabbitMQ y nunca debe apuntar a una base de desarrollo compartida o producción: las pruebas vacían sus tablas antes de cada caso.

GitHub Actions ejecuta el recorrido completo en Java 21 y 25 y construye la imagen con Java 21. La rama de trabajo es `feat/m8-notificaciones`, con PR propio hacia `main`.

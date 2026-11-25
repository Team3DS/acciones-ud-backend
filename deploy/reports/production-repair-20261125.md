# Reparación de integración en producción — 25/11/2026

El recorrido financiero QA reanudado en **Alpaca Paper** aprobó **42 comprobaciones, con 0 fallos y 0 bloqueos dentro de ese pase**, en 4.541 ms. Compra y venta quedaron ejecutadas y sus fondos y posiciones conciliados. El contrato de avisos de fondos M2 → M8 se corrigió, probó y desplegó en `b96cb0d`; los dos avisos QA quedaron recuperados en APP con sus UUID originales, sin nuevos correos ni efectos financieros. Corte final: **25/11/2026, 12:45:33, America/Bogota**.

El sitio es [accionesud.site](https://accionesud.site). Se conservan los ocho microservicios propietarios de sus datos, gateway, frontend, PostgreSQL, Redis, RabbitMQ y Caddy. Las operaciones se limitaron a una cuenta QA aislada y al entorno Paper; no hubo negociación live ni cargos Stripe.

## Resultado operativo

| Comprobación | Resultado |
| --- | --- |
| Compra QA 3 y venta QA 4 | Ambas EJECUTADAS; la recuperación no repitió movimientos ni fills. |
| Efectivo tras compra/venta | USD 998.54 desde USD 1,000.00 iniciales; comisiones USD 0.70 + USD 0.70 y pérdida bruta realizada USD 0.06. |
| Posición y reservas | Posición cerrada, cantidad reservada 0 y confirmaciones financieras pendientes 0. |
| LIMIT QA 5 | AAPL, una acción a USD 1, creada en Alpaca Paper y cancelada; repetir cancelación devolvió 204 y no dejó reserva. |
| Retiro interno QA de USD 10 | Reintento idempotente con el mismo movimiento; efectivo final USD 988.54, sin cambiar el reporte de negociación. |
| Notificaciones y preferencias | Marcar un aviso como leído devolvió 204; preferencias conservadas durante la prueba. Los dos avisos de fondos se recuperaron en APP; después se mantuvo CORREO desactivado solo para QA. |
| Sesión | Refresh inválido 401; logout inválido 401 sin revocar el válido; refresh válido 200; logout válido 200; refresh revocado 401. |

La comprobación final de salud de las **12:45:33, America/Bogota**, encontró gateway y M1/M2/M3/M4/M5/M6/M8/M10 UP, todos sin reinicios. Frontend, PostgreSQL, Redis y RabbitMQ estaban healthy y sin reinicios. Son diez componentes de aplicación al contar el frontend. HTTPS principal, planes y SockJS `/info` respondieron 200; horarios sin token respondió 401, según su protección de acceso. Los hashes de JAR runtime M2/M5/M6 coinciden con los paquetes indicados abajo.

## Defectos corregidos

- **M1:** consumo MFA atómico y control de intentos en Redis, protección de logout, validación de entrada, roles del perfil y contacto interno autenticado para M8.
- **M2:** consumo parcial y remanente de reservas, idempotencia entre REST/Rabbit, `finalExecution`, `expectedExecutionCount` y migración V5 aditiva. Un fill final adelantado espera los parciales que faltan; una orden parcialmente ejecutada no se libera mediante el TTL de una reserva sin ejecuciones.
- **M4 y gateway:** contratos de catálogo/horarios y transporte HTTP, SockJS y WebSocket. La ruta válida de horarios es `/api/v1/market/hours?market=NYSE` o `ALL`.
- **M5:** autenticación y contratos con participantes, conservación de reservas ante un envío ambiguo a Alpaca, recuperación por `client_order_id`, fechas opcionales de reportes en PostgreSQL, UUID `eventId` determinista por ejecución, lectura del coste histórico de M6 y redondeo de importes.
- **M6:** SELL conserva el coste previo a vender, aunque la posición quede en cero; BUY utiliza el promedio posterior. Respuesta, persistencia y outbox transmiten ese coste de ejecución. Los reintentos recuperan el coste persistido, incluso después de una adquisición posterior.
- **Avisos M2 → M8:** productor ampliado con usuario/orden y campos anteriores conservados. M8 ya acepta el contrato; sus nuevas pruebas cubren AMQP real, rechazo seguro, recuperación y deduplicación. No cambió el runtime M8 ni necesita desplegarse otro JAR de ese módulo.
- **M8:** JWT corto hacia M1/M3/M4 y token interno separado hacia M10, con propietarios validados y deduplicación por `eventId`.
- **Frontend:** resolución dinámica del gateway mediante DNS Docker cada diez segundos, conservando ruta y query en REST y WebSocket. La prueba Docker cambió la IP del gateway sin recargar Nginx y mantuvo los mismos workers. No cambió la interfaz visual.
- **Infraestructura:** recuperación automática de PostgreSQL/Redis/RabbitMQ y healthcheck PostgreSQL con usuario/base reales, preservando los ajustes locales del despliegue.

## Recuperación QA y control de duplicados

La venta total ya estaba aplicada en el broker, fondos y portafolio, pero M5 seguía ENVIADA: M6 había comunicado el coste cero de la posición cerrada como coste de ejecución. Se corrigió **solo el metadato de coste de la venta QA**, fila 2 en M6, de 0 a 349.86, con evidencia de la compra, guardas transaccionales y auditoría. No cambiaron balances, cantidad 0, reserva 0 ni PnL realizado -0.06. No se relajó una restricción para aceptar costes inválidos ni se repararon datos históricos generales.

M5 se detuvo durante el diagnóstico para contener los avisos repetidos y se desplegó corregido después de las pruebas. La auditoría encontró un acumulado posterior de 72 avisos repetidos de la venta, estable durante más de tres minutos; el último fue a las 12:27:36. Ese historial se conserva y no se purgó para aparentar ausencia de errores.

La limpieza final conservó el usuario QA y su historial. Solo su correo de prueba se sustituyó por un alias bajo guardas de identidad y propósito. El refresh estaba revocado; password y session se retiraron del archivo privado. Una futura ejecución QA requiere MFA nuevo. Este documento no contiene correos, credenciales, tokens ni OTP.
Después de verificar las preferencias, la limpieza desactivó **solo el canal CORREO de los diez tipos de aviso QA**, con copia previa privada. APP permanece intacto. Se evita así continuar enviando correos de prueba, sin modificar cuentas ajenas.

## Artefactos M2/M5/M6

Los hashes siguientes corresponden a los JAR Java 21 usados en la reparación; no son hashes de imágenes Docker.

| Módulo | Revisión | SHA-256 del JAR |
| --- | --- | --- |
| M2 | `b96cb0d` | `b6b58c9fe6094ab9415308479416f9f6bff87d2d4a4b4e809c44b706703243b8` |
| M5 | `6bb613f` | `8b36f5a5b76e20249e90574af4fad3270ac815f1870710b3f7604b2f2c95d49f` |
| M6 | `eb373c2` | `650d89e1d1ab0abb42240b087f73630c4d96e84f6329544808ca8f4d928fa5b8` |

M2, M5 y M6 se confirmaron UP con hashes runtime exactos. La rama de correcciones se publicó hasta `b96cb0d`. El intento anterior de crear un PR mediante el conector recibió 403, por lo que no se declara un PR existente.

## Validación

| Alcance | Resultado y evidencia |
| --- | --- |
| Recorrido financiero QA reanudado | `qa-resume.json`: 42 PASS, 0 FAIL, 0 bloqueadas; 4.541 ms. |
| Salud final | `qa-health-final.json`: backends UP y componentes comprobados sin reinicios. |
| Recuperación APP de fondos | `qa-m8-recovery-result.json`: dos eventos y dos avisos, cero propietarios incorrectos/correos nuevos, repetición idempotente y fondos sin cambio. |
| Limpieza QA | `qa-cleanup-result.json`, sanitizado. |
| API de lectura / autenticación HTTP | 29 PASS / 11 PASS, en etapas separadas. |
| M1 | 89 pruebas, sin fallos/errores/omisiones; siete casos Redis reales. |
| M2 | Paquete final: 275 pruebas aprobadas, 227 unitarias y 48 PostgreSQL/Rabbit; protección del TTL parcial conservada. |
| M5 | Paquete final: 310 pruebas aprobadas; seis PostgreSQL y dos de `eventId`. |
| M6 | Paquete: 270 pruebas aprobadas, incluidas dos PostgreSQL/Rabbit de coste histórico y reintento entre transportes. |
| M8 | 58 pruebas aprobadas: 51 unitarias y siete PostgreSQL/Rabbit del contrato de fondos, recuperación del mismo UUID, owner inválido, deduplicación y SQL40001 con rollback/reintento. |
| Frontend DNS | Imagen construida, `nginx -t` y regresión Docker HTTP/WebSocket aprobadas sin reload. |

Los resultados pertenecen a ejecuciones distintas y **no se suman como pruebas únicas del sistema**. El pase Paper inicial tuvo 51 PASS, 1 FAIL y cuatro bloqueadas; se conserva como evidencia del diagnóstico, no como resultado del pase reanudado. No se declara un reactor global completo aprobado.

Los informes JSON sanitizados y los logs de cada paquete se conservan como evidencia de ejecución. Los datos privados QA y los respaldos con configuración no deben adjuntarse a un PR ni a una entrega pública.

## Contrato de fondos y recuperación APP: verificados

- M2 `b96cb0d` quedó desplegado UP/healthy con el SHA exacto. Su paquete aprobó 275 pruebas; M8 aprobó 58, incluidas siete PostgreSQL/Rabbit del contrato, recuperación y reintentos. No cambió el runtime M8 ni fue necesario reemplazar su JAR.
- Recuperación PASS a las **12:44:31**: los dos avisos del contrato anterior se dirigieron exclusivamente a M8, con owner/orden acreditados mediante outbox exacto → reservas QA 1/2 → cuenta autorizada, órdenes 3/5. Se conservaron UUID y timestamp originales.
- Resultado: `processed=2`, `notifications=2`, `wrong_owner=0`, `mail=0`, `duplicate_delivery_idempotent=true`, `funds_unchanged=true`. El efectivo siguió en USD 988.54. La repetición no creó avisos adicionales, movimientos, fills ni operaciones de broker.
- La recuperación fue en APP, sin correo, conforme al fixture QA silencioso. No reactivó CORREO, no solicitó MFA y no publicó eventos financieros nuevos.
- Auditoría final de las **12:45:33**: colas operativas M2 movimientos, M5, M6 posiciones y M8 notificaciones/reintentos con `ready=0` y `unacked=0`. M2 DLQ=0; M6 DLQ=0.
- M5 conserva un mensaje histórico QA en DLQ por la confirmación con coste cero de las 12:08, cuya operación ya está conciliada. M8 conserva los dos originales, recuperados con sus mismos UUID y archivados de forma privada. No se purgaron las DLQ para aparentar ausencia de errores.
- Tres respuestas Resend 403 del 24/11 corresponden a otra cuenta, no al pase QA actual, y se preservaron sin alteración ni reintento. El registro no conserva la causa detallada; no se atribuyen a modo de pruebas ni a un fallo general actual. El dominio propio de envío es `accionesud.site`, no onboarding.

Contrato y recuperación **cerrados en el alcance descrito**. El resultado financiero de 42 comprobaciones permanece válido con su alcance. Evidencias: `qa-m8-recovery-result.json`, `qa-health-final.json`, `m2-notification-schema-final-package-green-20261125.log` y `m8-funds-contract-final-20261125.log`.

## Límites y alcance académico

- Stripe devuelve 401 con la credencial configurada: Checkout, pago y webhook que dependen de ella siguen bloqueados externamente.
- Revisión visual INCONCLUSA por la política de la herramienta al verificar la URL; no se eludió el bloqueo mediante otro canal UI.
- No se ejecutó carga de 1.500 usuarios ni se acreditan sus umbrales de respuesta. La duración del harness no sustituye un benchmark. Un respaldo existente tampoco demuestra una restauración ensayada.
- Se conservan arquitectura y acuerdos de Acciones UD/SolTech, negro/dorado y segundo logo aprobado, y presupuesto de proyecto USD 35.000. No se crean M7/M9 por inferencia.
- El SAD trabaja NYSE/NASDAQ, diez acciones y USD, pero DP-01 mantiene pendiente la ratificación del recorte frente al enunciado original. Idiomas, temas y políticas pendientes no se convierten en acuerdos tácitos.
- Esta reparación no certifica los 82 RF, 24 RNF, 48 CU ni los diez entregables académicos, y no garantiza una nota de 5.

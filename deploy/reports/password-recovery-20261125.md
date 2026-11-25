# Recuperación de contraseña — 25/11/2026

La recuperación mediante enlace de un solo uso quedó implementada, probada y desplegada en [accionesud.site](https://accionesud.site). El cierre del despliegue, a las **13:40:15, America/Bogota**, aprobó **19 comprobaciones, 0 fallos**, con los nueve backends UP, sin reinicios, y frontend healthy con `nginx -t` correcto. El recorrido de recuperación de una cuenta real queda a cargo de su titular; estas comprobaciones no cambiaron contraseñas ni enviaron correos a cuentas conocidas.

## Comportamiento y contratos

- `POST /api/v1/auth/password-reset/request`, con `{correo}`, devuelve 202 y el mismo mensaje neutro para cuentas desconocidas, cooldown y solicitudes aceptadas. No expone identidad, código ni contraseña.
- M1 genera un desafío separado del MFA: seis dígitos, cinco minutos, máximo cinco errores y un minuto entre solicitudes. El enlace usa `https://accionesud.site/#password-reset?correo=<codificado>&code=<seis-dígitos>`. El fragmento no se transmite en la solicitud HTTP ni en el encabezado Referer.
- M8 construye el enlace únicamente desde `APP_PUBLIC_URL`, configurada expresamente como `https://accionesud.site`. Rechaza una base sin HTTPS o con credenciales, ruta, query o fragmento. No toma el origen del payload ni del Host de una solicitud.
- El frontend retira el fragmento antes de montar la pantalla, conserva el código solo en memoria y pide la nueva contraseña dos veces. Permite el código manual. La clave se conserva exactamente como fue escrita: al menos ocho puntos de código, no solo espacios, y hasta 72 bytes UTF-8. No se trunca ni recorta.
- `POST /api/v1/auth/password-reset/confirm`, con `{correo,code,newPassword}`, responde 200 al cambiar la contraseña; datos inválidos reciben 400 y cuenta/código inexistente, vencido o usado reciben el mismo 401 genérico. Un error de política de clave no consume el desafío.
- Redis reserva el desafío de forma atómica. Se consume después del commit y se libera tras rollback. Login, verificación MFA, refresh y recuperación comparten el bloqueo de la fila del usuario para impedir carreras con la credencial anterior.
- El cambio guarda BCrypt, revoca refresh y desafíos MFA, y registra `m1.recuperacion.completada` en el outbox dentro de la misma transacción. El evento contiene solo `userId` y `email`; su publicación confirmada conserva UUID para deduplicación. M8 envía «Contraseña actualizada» al propietario, fuera del centro APP.
- Los correos de solicitud y cambio se almacenan cifrados, respetan la caducidad de entrega y eliminan el contenido tras el envío. Las pruebas verifican deduplicación y ausencia de códigos/enlaces en logs. El frontend limpia su sesión local después del cambio válido y dirige al login, sin iniciar sesión automáticamente.

## Evidencia y artefactos

| Alcance | Resultado |
| --- | --- |
| M1 | Package: 120 pruebas, 0 fallos/errores/omisiones; PostgreSQL, Redis y RabbitMQ reales, concurrencia, rollback y outbox. |
| M8 | Package: 72 pruebas, 0 fallos/errores/omisiones; 62 unitarias, siete PostgreSQL/Rabbit del contrato de fondos y tres PostgreSQL/WireMock de recuperación. |
| Frontend | 54 pruebas nativas aprobadas y build TypeScript/Vite/imagen correcto; sin recorrido visual automatizado. |
| Producción | `qa-password-recovery-deployment.json`: 19 PASS, 0 FAIL; corte 13:40:15. |

El smoke usó un correo comprobado como inexistente en la base: request genérico/repetido 202, validación 400, confirmación desconocida 401, perfil anónimo 401 y login desconocido 400. Comprobó `APP_PUBLIC_URL`, binding del aviso de cambio, hashes runtime M1/M8 y coincidencia de index/JavaScript con el ZIP probado, incluida la referencia relativa `./assets`. M8 arrancó UP, después M1 UP y finalmente frontend healthy. No se solicitó OTP ni contraseña al usuario para estas verificaciones.

| Artefacto | Revisión | SHA-256 |
| --- | --- | --- |
| JAR M1 | `118597a` | `de772fe3be3ad0532c582bc88fcae76e4183e5da0740e89f3dd4897f17cbbbe2` |
| JAR M8 | `118597a` | `5e57edc7fca3cd04c77b430bf058d5e9ff9b7ec4ad34ba283d41f9be78c6ed8c` |
| ZIP dist frontend | `cc99fd8`, local | `4e55a93ba43dedb5a0e7f37c4c9b7b0190a48fa20a0d7d9dee97cdb3e89753fd` |

Los hashes de JAR corresponden a los artefactos Java, no a imágenes Docker. M8 pesa 79.257.952 bytes y se generó a las 13:30:43; M1 pesa 75.190.317 bytes y se generó a las 13:31:42. El ZIP pesa 156.540 bytes. Los hashes runtime de M1/M8 coinciden con los paquetes.

La rama backend está publicada hasta `297126f`, incluido el código `118597a` y el parche frontend. Frontend `cc99fd8`, sobre `78d9cea`, quedó local: tanto el conector como el push de la rama recibieron 403. No se afirma que esa rama esté publicada. El cambio acumulativo desde `be8c566` se conserva en [frontend-password-recovery.patch](../patches/frontend-password-recovery.patch), validado contra la fuente final y aplicado al servidor. Se conservan respaldo protegido `password-recovery-20261125-KDemuo` y etiquetas de rollback de M1, M8 y frontend.

Logs: `output/m1-password-recovery-final-20261125.log`, `output/m8-password-recovery-final-20261125.log`, `output/frontend-password-reset-link-tests-20261125.log` y `output/frontend-password-reset-link-build-20261125.log`. El JSON sanitizado de despliegue se conserva con la evidencia operativa. Estos conteos corresponden a ejecuciones distintas y no se suman como pruebas únicas del sistema.

## Alcance académico y límites

RF-04/CU-05 ya cuentan con enlace de un uso, vigencia de cinco minutos dentro del máximo de treinta, respuesta neutra, confirmación doble de clave, cambio protegido, aviso posterior y retorno al login. El cambio no se considera probado con la cuenta real del usuario hasta que su titular complete el recorrido.

La revocación cubre refresh y desafíos MFA. Los JWT de acceso ya emitidos mantienen su caducidad actual; no hay invalidación inmediata de todos ellos. La expiración por treinta minutos de inactividad no está acreditada. En RF-02, invalidar el desafío después de cinco errores no acredita el bloqueo del usuario durante quince minutos exigido por el SAD; queda pendiente contrastarlo y resolverlo dentro del alcance aceptado.

Se conservan los ocho microservicios y los acuerdos del proyecto. Este cierre no acredita todos los RF/RNF/CU, carga, restauración, Stripe ni entregables académicos, y no garantiza una nota de 5. El recorrido financiero previo de 42 comprobaciones mantiene su alcance separado.

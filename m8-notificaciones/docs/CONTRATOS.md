# Contratos de M8

## Sobre RabbitMQ

Exchange topic durable: `acciones.ud`. Cola de M8: `m8.notificaciones`; errores: `m8.notificaciones.dlq`. El consumidor admite JSON, nunca deserialización Java de objetos recibidos por red.

```json
{
  "eventId": "91db054a-3d5b-4d54-b74b-7e680132266e",
  "eventType": "m2.movimiento.creado",
  "occurredAt": "2026-11-16T16:00:00Z",
  "payload": {"userId": 42, "movementId": 10}
}
```

El tipo debe coincidir con la routing key. `eventId` es UUID estable, no se genera un identificador nuevo al reintentar. Puede proceder de `messageId` si falta en el sobre. Se admite el payload como objeto JSON o texto que contiene JSON, para compatibilidad con adaptadores de outbox. El formato coincide con el sobre `EventoDominio` de M2. Un campo `source`, si está presente, debe coincidir con el origen deducido de la ruta.

| Origen | Tipos admitidos | Payload relevante |
| --- | --- | --- |
| M1 | `m1.usuario.registrado` | `userId`, `email` |
| M1 | `m1.mfa.solicitado`, `m1.verificacion.solicitada`, `m1.recuperacion.solicitada` | `userId`, `email`, `code` de seis dígitos, `expiresAt` UTC; no se guardan en el centro |
| M2 | `m2.movimiento.creado`, `m2.fondos.reservados`, `m2.fondos.confirmados`, `m2.fondos.liberados` | `userId` o `usuarioId`, identificadores de referencia opcionales |
| M4 | `m4.mercado.abierto`, `m4.mercado.cerrado`, `m4.mercado.horario.cambiado` | `market`: NYSE/NASDAQ; M8 distribuye a sus suscritos |
| M5 | `m5.orden.creada`, `m5.orden.ejecutada`, `m5.orden.cancelada`, `m5.orden.rechazada`, `m5.orden.actualizada` | `userId` o `usuarioId`, `orderId` opcional |
| M10 | `m10.suscripcion.activada`, `m10.suscripcion.cancelada`, `m10.suscripcion.vencida`, `m10.pago.fallido` | `userId` o `usuarioId`; cancelar/vencer desactiva las alertas |

El envío a usuarios que no consten en el contrato no se adivina a partir de una reserva u orden. En `main`, algunos payloads de reservas de M2 no incluyen `userId`: esos productores deben ampliarse antes de habilitar sus avisos. M1 genera OTP pero aún no publica estos eventos de entrega; esa conexión es trabajo de integración pendiente. El publicador heredado de M1 utiliza otro formato y no debe conectarse mediante deserialización Java insegura ni generando UUID aleatorios en M8.

## HTTP entre servicios

Todas las llamadas tienen tiempos máximos de conexión/lectura y utilizan `M8_SERVICE_TOKEN` cuando está configurado. El propietario debe autenticar y autorizar esa credencial; que M8 la envíe no acredita que esa autorización ya exista en el otro servicio. No hay accesos SQL a bases ajenas.

| Propietario | Solicitud | Respuesta mínima y estado de integración |
| --- | --- | --- |
| M3 | `GET /api/v1/instruments/id/{id}` | `{"id":1,"symbol":"AAPL","enabled":true,"type":"ACCION"}`. Ruta disponible en la rama de M3/PR #18, pendiente de integrarse a main |
| M4 | `GET /api/v1/quotes/{symbol}` | `{"symbol":"AAPL","price":151.25,"timestamp":"2026-11-16T16:00:00Z"}`. Contrato contrastado con la rama `feat/m4-datos-mercado-clean`; pendiente de integrar/desplegar |
| M10 | `GET /internal/subscriptions/{userId}/entitlement` | `{"active":true,"validUntil":"2026-12-01T00:00:00Z"}`. Contrato propuesto; M10 en main es un esqueleto. Ausencia, error o respuesta inválida no conceden premium |
| M1 | `GET /internal/users/{userId}/notification-contact` | `{"email":"usuario@example.test"}`. Contrato propuesto; no existe en main. Se consulta al preparar un correo de negocio sin destinatario en el evento |
| M1 | `GET /internal/users/monthly-report-subscribers?page=0&size=100` | `{"content":[{"userId":42,"email":"usuario@example.test"}]}`; página vacía termina la consulta. Contrato propuesto |
| M5 | `GET /internal/reports/monthly/{userId}?period=2026-10` | `{"grossProfit":12.5,"commissions":0.8,"currency":"USD"}`. Solo resultados confirmados; contrato propuesto |
| M6 | `GET /internal/portfolio/{userId}/summary?period=2026-10` | `{"assetsSummary":"AAPL: 2 acciones"}`. Resumen del cierre del periodo, contrato propuesto |

Los contratos propuestos están implementados del lado consumidor y probados con WireMock. Deben ratificarse con los responsables e implementarse en sus productores. Los servicios que aún son esqueletos no se presentan como integraciones reales operativas.

## Resend

`POST /emails` usa `Authorization: Bearer ...`, `Idempotency-Key: m8/{entregaId}`, `from`, `to`, `subject` y `html`. M8 requiere una respuesta exitosa con `id` no vacío para confirmar. El mismo identificador se conserva en todos los intentos. La bandeja limita la vida de cada entrega a menos de 24 horas; la documentación de Resend indica una retención de claves de 24 horas.

Referencias: [enviar correo](https://resend.com/docs/api-reference/emails/send-email), [idempotencia](https://resend.com/docs/dashboard/emails/idempotency-keys), [colas durables de RabbitMQ](https://www.rabbitmq.com/docs/queues).

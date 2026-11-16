# Eventos de catálogo M3

Exchange: `acciones.ud`, tipo topic, durable. Mensajes JSON persistentes con `messageId=eventId`. La routing key coincide con `eventType`.

| Routing key | Payload |
| --- | --- |
| `m3.mercado.creado` | code, mic, name, timezone, currency, country, tradingHours, enabled |
| `m3.mercado.actualizado` | Los mismos metadatos y habilitación después del cambio |
| `m3.instrumento.creado` | symbol, name, market, currency, lotSize, tickSize, type |
| `m3.instrumento.actualizado` | symbol y changes con los campos del PATCH |

El sobre contiene `eventId` UUID, `eventType`, `occurredAt` en UTC, `aggregateId` como string, `correlationId` y `payload`. Un mercado usa su código estable como aggregateId y un instrumento su ID numérico.

El productor escribe primero en outbox en la transacción del comando. El relay bloquea pendientes mediante SKIP LOCKED y espera publisher confirms. Un nack, timeout o devolución por falta de cola conserva el evento pendiente y programa reintento. Si el broker acepta pero la confirmación o la transacción local falla, puede haber repetición: el consumidor debe deduplicar por eventId.

M4 y otros consumidores deben declarar sus colas durables y bindings. La ausencia de colas no se interpreta como entrega correcta. La extensión `m3.mercado.actualizado`, necesaria para propagar cambios administrativos, se añade a los tres eventos inicialmente descritos por la wiki.

Las actualizaciones no cambian el símbolo ni trasladan el instrumento a otro mercado. La interpretación operativa de cambios de lote, tick, estado u horario corresponde al consumidor propietario del proceso. No se afirma que el consumidor actual de M4 esté implementado.

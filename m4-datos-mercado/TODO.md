# M4 Datos de Mercado - TODO

## Pendiente
- [ ] Value Object Cotizacion con validaciones de dominio (precio > 0, symbol no vacío)
- [ ] Entidades JPA HistoricoCotizacion y HorarioMercado completas con índices
- [ ] AlpacaMarketDataAdapter con WebClient + Resilience4j real
- [ ] RedisCacheAdapter con serialización JSON y TTL configurable
- [ ] WebSocket STOMP handler para tiempo real
- [ ] Scheduler de actualización de caché
- [ ] Consumidor RabbitMQ eventos InstrumentoCreado/Actualizado de M3
- [ ] OpenAPI docs completas con ejemplos
- [ ] Tests de integración con Testcontainers (Postgres, Redis) + WireMock Alpaca
- [ ] Coverage >= 70% JaCoCo
- [ ] Dockerfile multi-stage

## Hecho
- [x] Estructura hexagonal base
- [x] Domain: Cotizacion, HistoricoCotizacion, HorarioMercado, Intervalo, EstadoMercado
- [x] Ports out: AlpacaMarketDataPort, CacheCotizacionesPort, HistoricoRepositoryPort, CatalogoClientPort, WebSocketPort
- [x] Use cases: ObtenerCotizacionUseCase, ObtenerHistoricoUseCase, ConsultarHorariosUseCase
- [x] Adapters stub: RedisCacheAdapter, AlpacaMarketDataAdapter, CatalogoRestClient, JpaHistoricoRepositoryAdapter
- [x] Controllers REST: /api/v1/quotes/**, /api/v1/market/**
- [x] OpenApiConfig
- [x] Tests unitarios ObtenerCotizacionUseCase y HorarioMercado con edge cases

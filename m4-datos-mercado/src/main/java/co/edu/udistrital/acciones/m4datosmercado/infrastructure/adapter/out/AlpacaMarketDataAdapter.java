package co.edu.udistrital.acciones.m4datosmercado.infrastructure.adapter.out;

import co.edu.udistrital.acciones.m4datosmercado.application.port.out.AlpacaMarketDataPort;
import co.edu.udistrital.acciones.m4datosmercado.domain.Cotizacion;
import co.edu.udistrital.acciones.m4datosmercado.domain.HistoricoCotizacion;
import co.edu.udistrital.acciones.m4datosmercado.domain.Intervalo;
import co.edu.udistrital.acciones.m4datosmercado.infrastructure.adapter.out.dto.AlpacaQuoteDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Component
public class AlpacaMarketDataAdapter implements AlpacaMarketDataPort {
    private final WebClient webClient;
    private final String apiKey;
    private final String apiSecret;

    public AlpacaMarketDataAdapter(
            WebClient.Builder builder,
            @Value("${app.alpaca.api-key:}") String apiKey,
            @Value("${app.alpaca.api-secret:}") String apiSecret,
            @Value("${app.alpaca.base-url:https://paper-api.alpaca.markets}") String baseUrl) {
        this.webClient = builder.baseUrl(baseUrl).defaultHeaders(h -> {
            if (!apiKey.isBlank()) h.set("APCA-API-KEY-ID", apiKey);
            if (!apiSecret.isBlank()) h.set("APCA-API-SECRET-KEY", apiSecret);
        }).build();
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    @Override
    @CircuitBreaker(name = "alpaca")
    @Retry(name = "alpaca")
    public Cotizacion obtenerCotizacion(String symbol) {
        try {
            AlpacaQuoteDto dto = webClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/v2/stocks/{symbol}/quotes/latest").build(symbol))
                    .retrieve()
                    .bodyToMono(AlpacaQuoteDto.class)
                    .block();
            if (dto == null) {
                return fallback(symbol);
            }
            BigDecimal price = dto.bidPrice != null ? new BigDecimal(dto.bidPrice) : BigDecimal.ZERO;
            return new Cotizacion(
                    dto.symbol,
                    price,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    0L,
                    Instant.now(),
                    dto.bidPrice != null ? new BigDecimal(dto.bidPrice) : BigDecimal.ZERO,
                    dto.askPrice != null ? new BigDecimal(dto.askPrice) : BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO
            );
        } catch (Exception e) {
            return fallback(symbol);
        }
    }

    private Cotizacion fallback(String symbol) {
        return new Cotizacion(symbol, BigDecimal.valueOf(100), BigDecimal.ZERO, BigDecimal.ZERO, 0L, Instant.now(), BigDecimal.valueOf(99.9), BigDecimal.valueOf(100.1), BigDecimal.valueOf(101), BigDecimal.valueOf(99), BigDecimal.valueOf(100), BigDecimal.valueOf(99));
    }

    @Override
    public List<Cotizacion> obtenerCotizaciones(List<String> symbols) {
        return symbols.stream().map(this::obtenerCotizacion).toList();
    }

    @Override
    public List<HistoricoCotizacion> obtenerHistorico(String symbol, Instant from, Instant to, Intervalo intervalo) {
        return List.of();
    }
}

package co.edu.udistrital.acciones.m4datosmercado.infrastructure.adapter.out.rest;

import co.edu.udistrital.acciones.m4datosmercado.application.port.out.AlpacaMarketDataPort;
import co.edu.udistrital.acciones.m4datosmercado.domain.Cotizacion;
import co.edu.udistrital.acciones.m4datosmercado.domain.HistoricoCotizacion;
import co.edu.udistrital.acciones.m4datosmercado.domain.Intervalo;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
public class AlpacaMarketDataAdapter implements AlpacaMarketDataPort {

    private final WebClient webClient;
    private final String apiKey;
    private final String apiSecret;
    private final String dataFeed;
    /** Límite de espera por llamada. Reemplaza a @TimeLimiter, que solo aplica a métodos asíncronos. */
    private final Duration timeout;

    public AlpacaMarketDataAdapter(WebClient.Builder builder,
                                   @Value("${app.alpaca.base-url}") String baseUrl,
                                   @Value("${app.alpaca.api-key}") String apiKey,
                                   @Value("${app.alpaca.api-secret}") String apiSecret,
                                   @Value("${app.alpaca.data-feed}") String dataFeed,
                                   @Value("${app.alpaca.timeout-seconds:5}") long timeoutSeconds) {
        this.webClient = builder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.dataFeed = dataFeed;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    @Override
    @CircuitBreaker(name = "alpaca", fallbackMethod = "obtenerCotizacionFallback")
    @Retry(name = "alpaca")
    public Cotizacion obtenerCotizacion(String symbol) {
        Map<String, Object> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/stocks/quotes/latest")
                        .queryParam("symbols", symbol)
                        .queryParam("feed", dataFeed)
                        .build())
                .header("APCA-API-KEY-ID", apiKey)
                .header("APCA-API-SECRET-KEY", apiSecret)
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorMap(WebClientResponseException.class, ex -> new RuntimeException("Error Alpaca: " + ex.getResponseBodyAsString(), ex))
                .timeout(timeout)
                .block();

        return parseQuote(response, symbol);
    }

    @Override
    @CircuitBreaker(name = "alpaca", fallbackMethod = "obtenerCotizacionesFallback")
    @Retry(name = "alpaca")
    public List<Cotizacion> obtenerCotizaciones(List<String> symbols) {
        String symbolsParam = String.join(",", symbols);
        Map<String, Object> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/stocks/quotes/latest")
                        .queryParam("symbols", symbolsParam)
                        .queryParam("feed", dataFeed)
                        .build())
                .header("APCA-API-KEY-ID", apiKey)
                .header("APCA-API-SECRET-KEY", apiSecret)
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorMap(WebClientResponseException.class, ex -> new RuntimeException("Error Alpaca: " + ex.getResponseBodyAsString(), ex))
                .timeout(timeout)
                .block();

        return symbols.stream()
                .map(s -> parseQuote(response, s))
                .toList();
    }

    @Override
    @CircuitBreaker(name = "alpaca", fallbackMethod = "obtenerHistoricoFallback")
    @Retry(name = "alpaca")
    public List<HistoricoCotizacion> obtenerHistorico(String symbol, Instant from, Instant to, Intervalo intervalo) {
        String timeframe = mapIntervalo(intervalo);
        String start = formatAlpacaTime(from);
        String end = formatAlpacaTime(to);

        Map<String, Object> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/stocks/{symbol}/bars")
                        .queryParam("timeframe", timeframe)
                        .queryParam("start", start)
                        .queryParam("end", end)
                        .queryParam("feed", dataFeed)
                        .queryParam("limit", 10000)
                        .build(symbol))
                .header("APCA-API-KEY-ID", apiKey)
                .header("APCA-API-SECRET-KEY", apiSecret)
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorMap(WebClientResponseException.class, ex -> new RuntimeException("Error Alpaca histórico: " + ex.getResponseBodyAsString(), ex))
                .timeout(timeout)
                .block();

        return parseBars(response, symbol, intervalo);
    }

    private Cotizacion parseQuote(Map<String, Object> response, String symbol) {
        if (response == null || !response.containsKey("quotes")) {
            throw new RuntimeException("Respuesta inválida de Alpaca para " + symbol);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> quotes = (Map<String, Object>) response.get("quotes");
        Map<String, Object> quote = (Map<String, Object>) quotes.get(symbol);

        if (quote == null) {
            throw new RuntimeException("Cotización no encontrada para " + symbol);
        }

        BigDecimal price = new BigDecimal(quote.get("ap").toString());
        BigDecimal bid = new BigDecimal(quote.get("bp").toString());
        BigDecimal ask = new BigDecimal(quote.get("ap").toString());
        BigDecimal high = quote.get("h") != null ? new BigDecimal(quote.get("h").toString()) : null;
        BigDecimal low = quote.get("l") != null ? new BigDecimal(quote.get("l").toString()) : null;
        BigDecimal open = quote.get("o") != null ? new BigDecimal(quote.get("o").toString()) : null;
        BigDecimal prevClose = quote.get("pc") != null ? new BigDecimal(quote.get("pc").toString()) : null;
        Long volume = quote.get("v") != null ? Long.valueOf(quote.get("v").toString()) : 0L;
        Instant timestamp = Instant.parse(quote.get("t").toString());

        BigDecimal change = prevClose != null ? price.subtract(prevClose) : BigDecimal.ZERO;
        BigDecimal changePct = prevClose != null && prevClose.compareTo(BigDecimal.ZERO) != 0
                ? change.divide(prevClose, 4, java.math.RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;

        BigDecimal spread = ask.subtract(bid);
        BigDecimal marketCap = volume > 0 ? price.multiply(new BigDecimal(volume)) : null;

        return new Cotizacion(symbol, price, change, changePct, volume, timestamp, bid, ask, high, low, open, prevClose, spread, marketCap);
    }

    private List<HistoricoCotizacion> parseBars(Map<String, Object> response, String symbol, Intervalo intervalo) {
        if (response == null || !response.containsKey("bars")) {
            return List.of();
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> bars = (List<Map<String, Object>>) response.get("bars");

        return bars.stream()
                .map(bar -> {
                    Instant timestamp = Instant.parse(bar.get("t").toString());
                    BigDecimal open = new BigDecimal(bar.get("o").toString());
                    BigDecimal high = new BigDecimal(bar.get("h").toString());
                    BigDecimal low = new BigDecimal(bar.get("l").toString());
                    BigDecimal close = new BigDecimal(bar.get("c").toString());
                    Long volume = Long.valueOf(bar.get("v").toString());
                    return new HistoricoCotizacion(symbol, open, high, low, close, volume, timestamp, intervalo);
                })
                .toList();
    }

    private String mapIntervalo(Intervalo intervalo) {
        return switch (intervalo) {
            case M1 -> "1Min";
            case M5 -> "5Min";
            case M15 -> "15Min";
            case H1 -> "1Hour";
            case D1 -> "1Day";
            case ONE_WEEK -> "1Week";
            case ONE_MONTH -> "1Month";
            case THREE_MONTH -> "3Month";
            case ONE_YEAR -> "1Year";
        };
    }

    private String formatAlpacaTime(Instant instant) {
        return instant.atZone(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_INSTANT);
    }

    private Cotizacion obtenerCotizacionFallback(String symbol, Exception ex) {
        throw new RuntimeException("Servicio Alpaca no disponible para " + symbol, ex);
    }

    private List<Cotizacion> obtenerCotizacionesFallback(List<String> symbols, Exception ex) {
        throw new RuntimeException("Servicio Alpaca no disponible", ex);
    }

    private List<HistoricoCotizacion> obtenerHistoricoFallback(String symbol, Instant from, Instant to, Intervalo intervalo, Exception ex) {
        throw new RuntimeException("Servicio Alpaca no disponible para histórico", ex);
    }
}
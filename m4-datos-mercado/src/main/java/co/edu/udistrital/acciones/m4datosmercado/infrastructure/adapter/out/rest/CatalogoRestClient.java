package co.edu.udistrital.acciones.m4datosmercado.infrastructure.adapter.out.rest;

import co.edu.udistrital.acciones.m4datosmercado.application.port.out.CatalogoClientPort;
import co.edu.udistrital.acciones.m4datosmercado.domain.EstadoMercado;
import co.edu.udistrital.acciones.m4datosmercado.domain.HorarioMercado;
import co.edu.udistrital.acciones.m4datosmercado.infrastructure.security.EmisorTokenServicio;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

@Component
public class CatalogoRestClient implements CatalogoClientPort {

    private final WebClient webClient;
    private final long timeoutMs;

    private static final String TODOS = "ALL";
    private static final String MERCADO_POR_DEFECTO = "NYSE";

    public CatalogoRestClient(WebClient.Builder builder,
                              @Value("${app.m3.base-url}") String baseUrl,
                              @Value("${app.m3.timeout-ms}") long timeoutMs,
                              EmisorTokenServicio emisorToken) {
        // M3 exige token en todas sus consultas: cada petición lleva el token de sistema de M4.
        this.webClient = builder.baseUrl(baseUrl).filter(conToken(emisorToken)).build();
        this.timeoutMs = timeoutMs;
    }

    public static ExchangeFilterFunction conToken(EmisorTokenServicio emisorToken) {
        return (request, next) -> next.exchange(ClientRequest.from(request)
                .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + emisorToken.obtenerToken()))
                .build());
    }

    @Override
    public boolean validarSimbolo(String symbol) {
        // M3: GET /api/v1/instruments/{symbol}/validation -> {symbol, valid, reason, instrument}
        Map<String, Object> response = consultar("/api/v1/instruments/{symbol}/validation", symbol.toUpperCase());
        return response != null && Boolean.TRUE.equals(response.get("valid"));
    }

    @Override
    public Optional<HorarioMercado> obtenerHorarioMercado(String mercadoMic) {
        List<HorarioMercado> horarios = obtenerHorariosMercado(mercadoMic);
        return horarios.isEmpty() ? Optional.empty() : Optional.of(horarios.get(0));
    }

    /**
     * M3 publica el horario de referencia en el mercado: GET /api/v1/markets/{code} -> tradingHours {open, close}.
     * Se aplica de lunes a viernes. Sin mercado (o "ALL", como consulta el frontend) se usa NYSE.
     */
    @Override
    public List<HorarioMercado> obtenerHorariosMercado(String mercadoMic) {
        String codigo = mercadoMic == null || mercadoMic.isBlank() || TODOS.equalsIgnoreCase(mercadoMic)
                ? MERCADO_POR_DEFECTO : mercadoMic.toUpperCase();
        Map<String, Object> mercado = consultar("/api/v1/markets/{code}", codigo);
        if (mercado == null || !(mercado.get("tradingHours") instanceof Map<?, ?> horario)) {
            return List.of();
        }
        LocalTime apertura = hora(horario.get("open"));
        LocalTime cierre = hora(horario.get("close"));
        if (apertura == null || cierre == null) {
            return List.of();
        }
        String mic = mercado.get("mic") != null ? mercado.get("mic").toString() : codigo;
        return IntStream.rangeClosed(DayOfWeek.MONDAY.getValue(), DayOfWeek.FRIDAY.getValue())
                .mapToObj(dia -> new HorarioMercado(mic, dia, apertura, cierre, null, null))
                .toList();
    }

    @Override
    public EstadoMercado obtenerEstadoMercado(String mercadoMic, Instant instant) {
        return obtenerHorariosMercado(mercadoMic).stream()
                .filter(h -> h.esDiaLaborable(instant))
                .findFirst()
                .map(h -> h.obtenerEstado(instant))
                .orElse(EstadoMercado.CERRADO);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> consultar(String uri, Object... variables) {
        try {
            return webClient.get()
                    .uri(uri, variables)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .onErrorResume(WebClientResponseException.class, ex -> Mono.empty())
                    .block();
        } catch (Exception ex) {
            return null;
        }
    }

    /** Jackson puede serializar LocalTime como "09:30:00" o como [9, 30]. */
    static LocalTime hora(Object valor) {
        if (valor instanceof List<?> partes && partes.size() >= 2
                && partes.get(0) instanceof Number h && partes.get(1) instanceof Number m) {
            return LocalTime.of(h.intValue(), m.intValue());
        }
        try {
            return valor == null ? null : LocalTime.parse(valor.toString());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}

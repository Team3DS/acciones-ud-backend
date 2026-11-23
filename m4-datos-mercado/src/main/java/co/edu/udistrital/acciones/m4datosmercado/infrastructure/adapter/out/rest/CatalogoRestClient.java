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

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class CatalogoRestClient implements CatalogoClientPort {

    private final WebClient webClient;
    private final long timeoutMs;

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
        try {
            Map<String, Object> response = webClient.get()
                    .uri("/api/v1/instruments/validate", uriBuilder -> uriBuilder.queryParam("symbols", symbol).build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(java.time.Duration.ofMillis(timeoutMs))
                    .onErrorResume(WebClientResponseException.class, ex -> Mono.empty())
                    .block();

            if (response == null || !response.containsKey("validos")) {
                return false;
            }
            @SuppressWarnings("unchecked")
            List<String> validos = (List<String>) response.get("validos");
            return validos != null && validos.contains(symbol);
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public Optional<HorarioMercado> obtenerHorarioMercado(String mercadoMic) {
        List<HorarioMercado> horarios = obtenerHorariosMercado(mercadoMic);
        return horarios.isEmpty() ? Optional.empty() : Optional.of(horarios.get(0));
    }

    @Override
    public List<HorarioMercado> obtenerHorariosMercado(String mercadoMic) {
        try {
            Map<String, Object> response = webClient.get()
                    .uri("/api/v1/markets/{mic}/hours", mercadoMic)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(java.time.Duration.ofMillis(timeoutMs))
                    .onErrorResume(WebClientResponseException.class, ex -> Mono.empty())
                    .block();

            if (response == null || !response.containsKey("horarios")) {
                return List.of();
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> horariosData = (List<Map<String, Object>>) response.get("horarios");

            return horariosData.stream()
                    .map(this::mapToHorarioMercado)
                    .toList();
        } catch (Exception ex) {
            return List.of();
        }
    }

    @Override
    public EstadoMercado obtenerEstadoMercado(String mercadoMic, Instant instant) {
        List<HorarioMercado> horarios = obtenerHorariosMercado(mercadoMic);
        if (horarios.isEmpty()) {
            return EstadoMercado.CERRADO;
        }

        ZonedDateTime zdt = instant.atZone(ZoneId.of("America/New_York"));
        int diaSemana = zdt.getDayOfWeek().getValue();

        return horarios.stream()
                .filter(h -> h.getDiaSemana() == diaSemana)
                .findFirst()
                .map(h -> h.obtenerEstado(instant))
                .orElse(EstadoMercado.CERRADO);
    }

    private HorarioMercado mapToHorarioMercado(Map<String, Object> data) {
        String mic = (String) data.get("mercadoMic");
        int diaSemana = ((Number) data.get("diaSemana")).intValue();
        LocalTime apertura = LocalTime.parse(data.get("apertura").toString());
        LocalTime cierre = LocalTime.parse(data.get("cierre").toString());
        LocalTime preMarket = data.get("preMarket") != null ? LocalTime.parse(data.get("preMarket").toString()) : null;
        LocalTime afterHours = data.get("afterHours") != null ? LocalTime.parse(data.get("afterHours").toString()) : null;

        return new HorarioMercado(mic, diaSemana, apertura, cierre, preMarket, afterHours);
    }
}
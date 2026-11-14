package co.edu.udistrital.acciones.m4datosmercado.infrastructure.adapter.out;

import co.edu.udistrital.acciones.m4datosmercado.application.port.out.MercadoClientPort;
import co.edu.udistrital.acciones.m4datosmercado.domain.HorarioMercado;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Stream;

@Component
public class MercadoRestClient implements MercadoClientPort {

    private final WebClient webClient;

    public MercadoRestClient(WebClient.Builder builder,
                             @org.springframework.beans.factory.annotation.Value("${app.catalog.base-url:http://m3-catalogo-mercados:8083}") String baseUrl) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public List<HorarioMercado> obtenerHorarios() {
        try {
            List<MercadoDto> mercados = webClient.get()
                    .uri("/api/v1/markets")
                    .retrieve()
                    .bodyToFlux(MercadoDto.class)
                    .collectList()
                    .block();
            if (mercados == null) {
                return List.of();
            }
            return mercados.stream()
                    .filter(MercadoDto::enabled)
                    .flatMap(m -> diasSemana().stream()
                            .map(dia -> new HorarioMercado(
                                    null,
                                    m.mic(),
                                    dia,
                                    m.open(),
                                    m.close(),
                                    null,
                                    null)))
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<Integer> diasSemana() {
        return List.of(
                1, 2, 3, 4, 5);
    }

    public record MercadoDto(String code, String mic, String name, String timezone,
                             String currency, boolean enabled, String country,
                             HorarioReferenciaDto tradingHours) {
        public LocalTime open() {
            return tradingHours != null && tradingHours.open() != null
                    ? tradingHours.open()
                    : LocalTime.of(14, 30);
        }

        public LocalTime close() {
            return tradingHours != null && tradingHours.close() != null
                    ? tradingHours.close()
                    : LocalTime.of(21, 0);
        }
    }

    public record HorarioReferenciaDto(LocalTime open, LocalTime close) {}
}

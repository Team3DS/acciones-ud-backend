package co.edu.udistrital.acciones.m4datosmercado.infrastructure.adapter.out;

import co.edu.udistrital.acciones.m4datosmercado.application.port.out.CatalogoClientPort;
import co.edu.udistrital.acciones.m4datosmercado.application.service.CatalogoEventoService;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Set;

@Component
public class CatalogoRestClient implements CatalogoClientPort {

    private final WebClient webClient;
    private final CatalogoEventoService servicioEventos;

    public CatalogoRestClient(WebClient.Builder builder,
                              @org.springframework.beans.factory.annotation.Value("${app.catalog.base-url:http://m3-catalogo-mercados:8083}") String baseUrl,
                              CatalogoEventoService servicioEventos) {
        this.webClient = builder.baseUrl(baseUrl).build();
        this.servicioEventos = servicioEventos;
    }

    @Override
    public boolean simboloExiste(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return false;
        }
        // Primero intentar desde eventos de RabbitMQ (M3 publicado)
        if (servicioEventos.simboloExiste(symbol)) {
            return true;
        }
        // Luego intentar REST directo a M3
        try {
            ValidacionDto resultado = webClient.get()
                    .uri(uri -> uri.path("/api/v1/instruments/{symbol}/validation").build(symbol.toUpperCase()))
                    .retrieve()
                    .bodyToMono(ValidacionDto.class)
                    .block();
            return resultado != null && resultado.valid();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<String> validarSimbolos(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return List.of();
        }
        // Primero filtrar por lo que sabemos de eventos RabbitMQ
        List<String> desdeEventos = symbols.stream()
                .filter(s -> servicioEventos.simboloExiste(s))
                .toList();
        // Luego validar el resto por REST contra M3
        List<String> pendientes = symbols.stream()
                .filter(s -> !desdeEventos.contains(s))
                .toList();
        if (pendientes.isEmpty()) {
            return desdeEventos;
        }
        try {
            ValidacionRequest request = new ValidacionRequest(pendientes.stream().map(String::toUpperCase).toList());
            List<ValidacionDto> resultados = webClient.post()
                    .uri("/api/v1/instruments/validate")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToFlux(ValidacionDto.class)
                    .collectList()
                    .block();
            if (resultados == null) {
                return desdeEventos;
            }
            List<String> validosRest = resultados.stream()
                    .filter(ValidacionDto::valid)
                    .map(ValidacionDto::symbol)
                    .toList();
            List<String> combinado = new java.util.ArrayList<>(desdeEventos);
            combinado.addAll(validosRest);
            return combinado;
        } catch (Exception e) {
            return desdeEventos;
        }
    }

    public record ValidacionRequest(List<String> symbols) {}

    public record ValidacionDto(String symbol, boolean valid, String reason) {}
}

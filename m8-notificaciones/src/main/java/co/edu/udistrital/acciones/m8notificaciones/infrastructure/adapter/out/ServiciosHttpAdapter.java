package co.edu.udistrital.acciones.m8notificaciones.infrastructure.adapter.out;

import co.edu.udistrital.acciones.m8notificaciones.application.port.out.ServiciosPort;
import co.edu.udistrital.acciones.m8notificaciones.domain.AvisoException;

import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Component
public class ServiciosHttpAdapter implements ServiciosPort {
    private final RestClient http;
    private final String m1;
    private final String m3;
    private final String m4;
    private final String m5;
    private final String m6;
    private final String m10;
    private final Clock clock;

    public ServiciosHttpAdapter(
            @Value("${app.services.m1}") String m1,
            @Value("${app.services.m3}") String m3,
            @Value("${app.services.m4}") String m4,
            @Value("${app.services.m5}") String m5,
            @Value("${app.services.m6}") String m6,
            @Value("${app.services.m10}") String m10,
            @Value("${app.services.token:}") String token,
            Clock clock) {
        this.m1 = m1;
        this.m3 = m3;
        this.m4 = m4;
        this.m5 = m5;
        this.m6 = m6;
        this.m10 = m10;
        this.clock = clock;
        var factory =
                new JdkClientHttpRequestFactory(
                        HttpClient.newBuilder()
                                .version(HttpClient.Version.HTTP_1_1)
                                .connectTimeout(Duration.ofSeconds(3))
                                .build());
        factory.setReadTimeout(Duration.ofSeconds(4));
        var builder = RestClient.builder().requestFactory(factory);
        if (!token.isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + token);
        }
        http = builder.build();
    }

    @Override
    public boolean premium(long user) {
        JsonNode response = get(m10, "/internal/subscriptions/{id}/entitlement", user);
        if (!response.path("active").isBoolean()) {
            throw unavailable();
        }
        if (!response.path("active").asBoolean()) {
            return false;
        }
        try {
            return Instant.parse(required(response, "validUntil")).isAfter(clock.instant());
        } catch (java.time.format.DateTimeParseException exception) {
            throw unavailable();
        }
    }

    @Override
    public Instrumento instrumento(long id) {
        JsonNode response = get(m3, "/api/v1/instruments/id/{id}", id);
        if (response.has("type") && !"ACCION".equals(response.path("type").asText())) {
            throw new AvisoException(422, "Las alertas de este alcance requieren una acción.");
        }
        String symbol = required(response, "symbol");
        if (response.path("id").asLong() != id
                || !symbol.matches("[A-Z0-9][A-Z0-9.\\-]{0,19}")
                || !response.path("enabled").isBoolean()) {
            throw unavailable();
        }
        return new Instrumento(id, symbol, response.path("enabled").asBoolean());
    }

    @Override
    public Cotizacion cotizacion(String symbol) {
        JsonNode response = get(m4, "/api/v1/quotes/{symbol}", symbol);
        if (!symbol.equals(response.path("symbol").asText())
                || !response.path("price").isNumber()
                || response.path("price").decimalValue().signum() <= 0) {
            throw unavailable();
        }
        try {
            return new Cotizacion(
                    response.path("price").decimalValue(),
                    Instant.parse(required(response, "timestamp")));
        } catch (java.time.format.DateTimeParseException exception) {
            throw unavailable();
        }
    }

    @Override
    public String correoUsuario(long user) {
        String email =
                required(get(m1, "/internal/users/{id}/notification-contact", user), "email");
        if (!email.matches("[^\\s@<>]+@[^\\s@<>]+\\.[^\\s@<>]+") || email.length() > 254) {
            throw unavailable();
        }
        return email;
    }

    @Override
    public List<Destinatario> suscriptoresReporte(int page) {
        JsonNode content =
                get(m1, "/internal/users/monthly-report-subscribers?page={page}&size=100", page)
                        .path("content");
        if (!content.isArray() || content.size() > 100) {
            throw unavailable();
        }
        List<Destinatario> users = new ArrayList<>();
        for (JsonNode user : content) {
            long id = user.path("userId").asLong();
            String email = required(user, "email");
            if (id <= 0 || !email.matches("[^\\s@<>]+@[^\\s@<>]+\\.[^\\s@<>]+")) {
                throw unavailable();
            }
            users.add(new Destinatario(id, email));
        }
        return List.copyOf(users);
    }

    @Override
    public ResumenMensual resumen(long user, YearMonth month) {
        JsonNode financials =
                get(m5, "/internal/reports/monthly/{id}?period={period}", user, month.toString());
        JsonNode portfolio =
                get(m6, "/internal/portfolio/{id}/summary?period={period}", user, month.toString());
        if (!financials.path("grossProfit").isNumber()
                || !financials.path("commissions").isNumber()) {
            throw unavailable();
        }
        return new ResumenMensual(
                financials.path("grossProfit").decimalValue(),
                financials.path("commissions").decimalValue(),
                required(financials, "currency"),
                required(portfolio, "assetsSummary"));
    }

    private JsonNode get(String base, String path, Object... values) {
        try {
            var uri =
                    UriComponentsBuilder.fromUriString(base)
                            .path(path.contains("?") ? path.substring(0, path.indexOf('?')) : path);
            if (path.contains("?")) {
                uri.query(path.substring(path.indexOf('?') + 1));
            }
            JsonNode response =
                    http.get()
                            .uri(uri.buildAndExpand(values).encode().toUri())
                            .retrieve()
                            .body(JsonNode.class);
            if (response == null || !response.isObject()) {
                throw unavailable();
            }
            return response;
        } catch (RestClientException exception) {
            throw unavailable();
        }
    }

    private static String required(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw unavailable();
        }
        return value.asText();
    }

    private static AvisoException unavailable() {
        return new AvisoException(
                503, "Servicio de origen no disponible o respuesta incompatible.");
    }
}

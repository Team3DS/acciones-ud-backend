package co.edu.udistrital.acciones.m8notificaciones.infrastructure.adapter.out;

import co.edu.udistrital.acciones.m8notificaciones.application.port.out.CorreoException;
import co.edu.udistrital.acciones.m8notificaciones.application.port.out.CorreoPort;
import co.edu.udistrital.acciones.m8notificaciones.domain.Correo;

import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class ResendCorreoAdapter implements CorreoPort {
    private final RestClient http;
    private final String from;
    private final String apiKey;

    public ResendCorreoAdapter(
            @Value("${app.resend.base-url}") String base,
            @Value("${app.resend.api-key:}") String apiKey,
            @Value("${app.resend.from}") String from) {
        this.from = from;
        this.apiKey = apiKey;
        var factory =
                new JdkClientHttpRequestFactory(
                        HttpClient.newBuilder()
                                .version(HttpClient.Version.HTTP_1_1)
                                .connectTimeout(Duration.ofSeconds(3))
                                .build());
        factory.setReadTimeout(Duration.ofSeconds(5));
        http = RestClient.builder().baseUrl(base).requestFactory(factory).build();
    }

    @Override
    public String enviar(UUID id, Correo mail) {
        if (apiKey.isBlank() || from.isBlank()) {
            throw new CorreoException("RESEND_NO_CONFIGURADO", true);
        }
        if (mail.to() == null || !mail.to().matches("[^\\s@<>]+@[^\\s@<>]+\\.[^\\s@<>]+")) {
            throw new CorreoException("DESTINATARIO_INVALIDO", false);
        }
        try {
            JsonNode response =
                    http.post()
                            .uri("/emails")
                            .header("Authorization", "Bearer " + apiKey)
                            .header("Idempotency-Key", "m8/" + id)
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .body(
                                    Map.of(
                                            "from",
                                            from,
                                            "to",
                                            List.of(mail.to()),
                                            "subject",
                                            mail.subject(),
                                            "html",
                                            mail.html()))
                            .retrieve()
                            .body(JsonNode.class);
            if (response == null
                    || !response.path("id").isTextual()
                    || response.path("id").asText().isBlank()
                    || response.path("id").asText().length() > 100) {
                throw new CorreoException("RESPUESTA_RESEND_INVALIDA", true);
            }
            return response.path("id").asText();
        } catch (RestClientResponseException exception) {
            int code = exception.getStatusCode().value();
            throw new CorreoException("RESEND_HTTP_" + code, code == 429 || code >= 500);
        } catch (ResourceAccessException exception) {
            throw new CorreoException("RESEND_NO_DISPONIBLE", true);
        } catch (org.springframework.web.client.RestClientException exception) {
            throw new CorreoException("RESPUESTA_RESEND_INVALIDA", true);
        }
    }
}

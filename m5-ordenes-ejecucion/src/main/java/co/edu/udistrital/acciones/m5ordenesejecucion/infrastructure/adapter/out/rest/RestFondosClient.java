package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.rest;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.FondosClientPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.security.EmisorTokenInterno;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class RestFondosClient implements FondosClientPort {

    private static final Logger log = LoggerFactory.getLogger(RestFondosClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final EmisorTokenInterno emisorToken;

    public RestFondosClient(RestTemplate restTemplate,
                            @Value("${app.m2-client.base-url:http://m2-cuentas-fondos:8082}") String baseUrl,
                            EmisorTokenInterno emisorToken) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.emisorToken = emisorToken;
    }

    @Override
    public Long reservarFondos(String orderId, Long userId, BigDecimal amount, String idempotencyKey) {
        String url = baseUrl + "/api/v1/funds/reservations";
        Map<String, Object> body = Map.of(
                "orderId", orderId,
                "userId", userId,
                "amount", amount.setScale(2, RoundingMode.HALF_UP),
                "idempotencyKey", idempotencyKey
        );

        return ReservaRemota.solicitar(restTemplate, url, userId, emisorToken.paraUsuario(userId), body, "M2", "los fondos");
    }

    @Override
    public void confirmarReserva(Long reservationId, Long userId, String executionId, BigDecimal amount,
                                  boolean finalExecution, Integer expectedExecutionCount) {
        String url = baseUrl + "/api/v1/funds/reservations/" + reservationId + "/confirm";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(emisorToken.paraUsuario(userId));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("executionId", executionId);
        body.put("amount", amount.setScale(2, RoundingMode.HALF_UP));
        body.put("finalExecution", finalExecution);
        if (expectedExecutionCount != null) {
            body.put("expectedExecutionCount", expectedExecutionCount);
        }

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        try {
            restTemplate.postForObject(url, request, Map.class);
        } catch (Exception ex) {
            log.warn("Error comunicando con M2 confirmarReserva: {}", ex.getMessage());
            throw new IllegalStateException("No se pudo confirmar la reserva de fondos " + reservationId, ex);
        }
    }

    @Override
    public void liberarReserva(Long reservationId, Long userId, String reason) {
        String url = baseUrl + "/api/v1/funds/reservations/" + reservationId + "/release";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(emisorToken.paraUsuario(userId));

        Map<String, Object> body = Map.of("reason", reason);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        try {
            restTemplate.postForObject(url, request, Map.class);
        } catch (Exception ex) {
            log.warn("Error comunicando con M2 liberarReserva: {}", ex.getMessage());
            throw new IllegalStateException("No se pudo liberar la reserva de fondos " + reservationId, ex);
        }
    }
}

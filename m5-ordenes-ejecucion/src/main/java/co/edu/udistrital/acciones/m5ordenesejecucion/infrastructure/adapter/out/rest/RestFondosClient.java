package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.rest;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.FondosClientPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class RestFondosClient implements FondosClientPort {

    private static final Logger log = LoggerFactory.getLogger(RestFondosClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public RestFondosClient(RestTemplate restTemplate,
                            @Value("${app.m2-client.base-url:http://m2-cuentas-fondos:8082}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Long reservarFondos(String orderId, Long userId, BigDecimal amount, String idempotencyKey) {
        String url = baseUrl + "/api/v1/funds/reservations";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", String.valueOf(userId));
        headers.set("X-User-Roles", "USER");

        Map<String, Object> body = Map.of(
                "orderId", orderId,
                "userId", userId,
                "amount", amount,
                "idempotencyKey", idempotencyKey
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        // Sin reserva confirmada por M2 no hay orden: un error aquí hace que CrearOrden la marque RECHAZADA.
        Map<String, Object> response;
        try {
            response = restTemplate.postForObject(url, request, Map.class);
        } catch (Exception ex) {
            log.warn("M2 no reservó fondos para la orden {} ({}): {}", orderId, url, ex.getMessage());
            throw new IllegalStateException("No se pudieron reservar los fondos: " + ex.getMessage(), ex);
        }
        if (response != null && response.get("reservationId") instanceof Number num) {
            return num.longValue();
        }
        throw new IllegalStateException("M2 no devolvió reservationId para la orden " + orderId);
    }

    @Override
    public void confirmarReserva(Long reservationId, String executionId, BigDecimal amount) {
        String url = baseUrl + "/api/v1/funds/reservations/" + reservationId + "/confirm";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Roles", "USER,ADMIN");

        Map<String, Object> body = Map.of(
                "executionId", executionId,
                "amount", amount
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        try {
            restTemplate.postForObject(url, request, Map.class);
        } catch (Exception ex) {
            log.warn("Error comunicando con M2 confirmarReserva: {}", ex.getMessage());
        }
    }

    @Override
    public void liberarReserva(Long reservationId, String reason) {
        String url = baseUrl + "/api/v1/funds/reservations/" + reservationId + "/release";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Roles", "USER,ADMIN");

        Map<String, Object> body = Map.of("reason", reason);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        try {
            restTemplate.postForObject(url, request, Map.class);
        } catch (Exception ex) {
            log.warn("Error comunicando con M2 liberarReserva: {}", ex.getMessage());
        }
    }
}

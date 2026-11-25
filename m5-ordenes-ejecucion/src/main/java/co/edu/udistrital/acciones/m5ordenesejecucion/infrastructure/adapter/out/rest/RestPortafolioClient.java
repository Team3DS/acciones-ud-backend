package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.rest;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.PortafolioClientPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.LadoOrden;
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
import java.time.Instant;
import java.util.Map;

@Component
public class RestPortafolioClient implements PortafolioClientPort {

    private static final Logger log = LoggerFactory.getLogger(RestPortafolioClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final EmisorTokenInterno emisorToken;

    public RestPortafolioClient(RestTemplate restTemplate,
                                @Value("${app.m6-client.base-url:http://m6-portafolio:8086}") String baseUrl,
                                EmisorTokenInterno emisorToken) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.emisorToken = emisorToken;
    }

    @Override
    public Long reservarPosicion(String orderId, Long userId, String symbol, Integer quantity, String idempotencyKey) {
        String url = baseUrl + "/api/v1/positions/reservations";
        Map<String, Object> body = Map.of(
                "orderId", orderId,
                "userId", userId,
                "symbol", symbol,
                "quantity", BigDecimal.valueOf(quantity),
                "idempotencyKey", idempotencyKey
        );

        return ReservaRemota.solicitar(restTemplate, url, userId, emisorToken.paraUsuario(userId), body, "M6", "la posición");
    }

    @Override
    @SuppressWarnings("unchecked")
    public BigDecimal actualizarPosicion(String orderId, String executionId, Long userId, String symbol,
                                         LadoOrden side, Integer quantity, BigDecimal price,
                                         Instant executedAt, String eventId) {
        String url = baseUrl + "/api/v1/positions/updates";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(emisorToken.paraUsuario(userId));

        String ladoM6 = side == LadoOrden.COMPRA ? "BUY" : "SELL";

        Map<String, Object> body = Map.of(
                "orderId", orderId,
                "executionId", executionId,
                "userId", userId,
                "symbol", symbol,
                "side", ladoM6,
                "quantity", BigDecimal.valueOf(quantity),
                "price", price,
                "executedAt", executedAt != null ? executedAt.toString() : Instant.now().toString(),
                "eventId", eventId
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        try {
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);
            if (response != null && response.containsKey("position")) {
                Map<String, Object> pos = (Map<String, Object>) response.get("position");
                if (pos != null && pos.containsKey("averageCost")) {
                    Object ac = pos.get("averageCost");
                    return new BigDecimal(ac.toString());
                }
            }
        } catch (Exception ex) {
            log.warn("Error comunicando con M6 actualizarPosicion ({}): {}", url, ex.getMessage());
            throw new IllegalStateException("No se pudo actualizar la posición de la ejecución " + executionId, ex);
        }
        throw new IllegalStateException("M6 no devolvió averageCost para la ejecución " + executionId);
    }

    @Override
    public void liberarReserva(Long reservationId, Long userId, String reason) {
        String url = baseUrl + "/api/v1/positions/reservations/" + reservationId + "/release";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(emisorToken.paraUsuario(userId));

        Map<String, Object> body = Map.of("reason", reason);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        try {
            restTemplate.postForObject(url, request, Map.class);
        } catch (Exception ex) {
            log.warn("Error comunicando con M6 liberarReserva: {}", ex.getMessage());
            throw new IllegalStateException("No se pudo liberar la reserva de posición " + reservationId, ex);
        }
    }
}

package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Solicitud de reserva compartida por M2 (fondos) y M6 (posición).
 * Sin una reserva confirmada no hay orden: cualquier error se lanza para que CrearOrden la marque RECHAZADA.
 */
final class ReservaRemota {

    private static final Logger log = LoggerFactory.getLogger(ReservaRemota.class);

    private ReservaRemota() {
    }

    @SuppressWarnings("unchecked")
    static Long solicitar(RestTemplate restTemplate, String url, Long userId, String token, Map<String, Object> body,
                          String servicio, String recurso) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // M2 y M6 solo dejan reservar al dueño de la cuenta: el token va a nombre del usuario de la orden.
        headers.setBearerAuth(token);
        headers.set("X-User-Id", String.valueOf(userId));
        headers.set("X-User-Roles", "USER");
        Object orderId = body.get("orderId");

        Map<String, Object> response;
        try {
            response = restTemplate.postForObject(url, new HttpEntity<>(body, headers), Map.class);
        } catch (Exception ex) {
            log.warn("{} no reservó {} para la orden {} ({}): {}", servicio, recurso, orderId, url, ex.getMessage());
            throw new IllegalStateException("No se pudo reservar " + recurso + ": " + ex.getMessage(), ex);
        }
        if (response != null && response.get("reservationId") instanceof Number num) {
            return num.longValue();
        }
        throw new IllegalStateException(servicio + " no devolvió reservationId para la orden " + orderId);
    }
}

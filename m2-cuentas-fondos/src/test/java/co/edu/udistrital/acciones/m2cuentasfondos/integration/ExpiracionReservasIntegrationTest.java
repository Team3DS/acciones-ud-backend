package co.edu.udistrital.acciones.m2cuentasfondos.integration;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.RoutingKeys;
import co.edu.udistrital.acciones.m2cuentasfondos.application.usecase.ExpirarReservasUseCase;
import co.edu.udistrital.acciones.m2cuentasfondos.support.TokenTestFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@TestPropertySource(properties = {
        "app.reservas.ttl=PT1S",
        "app.reservas.expiracion.habilitada=false"
})
class ExpiracionReservasIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ExpirarReservasUseCase expirarReservas;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void unaReservaVencidaExpiraYDevuelveElDisponible() throws Exception {
        long userId = 9911L;
        depositar(userId, new BigDecimal("500.00"), "exp-dep-9911");
        long reservaId = reservar(userId, "orden-exp-9911", new BigDecimal("200.00"), "exp-res-9911");

        await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> assertThat(expirarReservas.ejecutar()).isEqualTo(1));

        assertThat(jdbcTemplate.queryForObject(
                "select estado from reserva where id = ?", String.class, reservaId)).isEqualTo("EXPIRED");
        assertThat(jdbcTemplate.queryForObject(
                "select motivo_liberacion from reserva where id = ?", String.class, reservaId))
                .isEqualTo("EXPIRADA");

        Map<String, Object> cuenta = jdbcTemplate.queryForMap(
                "select saldo_disponible, saldo_reservado, saldo_contable from cuenta where user_id = ?", userId);
        assertThat(numero(cuenta.get("saldo_disponible"))).isEqualByComparingTo("500.00");
        assertThat(numero(cuenta.get("saldo_reservado"))).isEqualByComparingTo("0.00");
        assertThat(numero(cuenta.get("saldo_contable"))).isEqualByComparingTo("500.00");

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from movimiento where idempotency_key = ?", Long.class, "LIB:" + reservaId))
                .isEqualTo(1L);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            String payload = jdbcTemplate.queryForObject(
                    "select payload from outbox_evento where aggregate_id = ? and event_type = ? "
                            + "order by creado_en desc limit 1",
                    String.class, String.valueOf(reservaId), RoutingKeys.FONDOS_LIBERADOS);
            assertThat(objectMapper.readTree(payload).get("reason").asText()).isEqualTo("EXPIRADA");
        });

        long movimientosAntes = jdbcTemplate.queryForObject(
                "select count(*) from movimiento where cuenta_id = (select id from cuenta where user_id = ?)",
                Long.class, userId);

        ResponseEntity<Map> liberacion = rest.exchange(
                "/api/v1/funds/reservations/" + reservaId + "/release", HttpMethod.POST,
                new HttpEntity<>(Map.of("reason", "LiberacionTardia"), cabeceras(userId)),
                Map.class);

        assertThat(liberacion.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(liberacion.getBody().get("status")).isEqualTo("EXPIRED");
        assertThat(numero(liberacion.getBody().get("available"))).isEqualByComparingTo("500.00");
        assertThat(numero(liberacion.getBody().get("reserved"))).isEqualByComparingTo("0.00");
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from movimiento where cuenta_id = (select id from cuenta where user_id = ?)",
                Long.class, userId)).isEqualTo(movimientosAntes);
        assertThat(jdbcTemplate.queryForObject(
                "select motivo_liberacion from reserva where id = ?", String.class, reservaId))
                .isEqualTo("EXPIRADA");
    }

    private void depositar(long userId, BigDecimal monto, String clave) {
        HttpHeaders headers = cabeceras(1L, "ADMINISTRADOR");
        headers.set("Idempotency-Key", clave);
        ResponseEntity<Map> respuesta = rest.exchange("/api/v1/funds/deposits", HttpMethod.POST,
                new HttpEntity<>(Map.of("userId", userId, "amount", monto), headers), Map.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private long reservar(long userId, String orden, BigDecimal monto, String clave) {
        HttpHeaders headers = cabeceras(userId, "INVERSIONISTA");
        headers.set("Idempotency-Key", clave);
        ResponseEntity<Map> respuesta = rest.exchange("/api/v1/funds/reservations", HttpMethod.POST,
                new HttpEntity<>(Map.of("orderId", orden, "userId", userId, "amount", monto), headers), Map.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return ((Number) respuesta.getBody().get("reservationId")).longValue();
    }

    private HttpHeaders cabeceras(long userId, String... roles) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(TokenTestFactory.token(userId, roles));
        return headers;
    }

    private BigDecimal numero(Object valor) {
        return new BigDecimal(valor.toString());
    }
}

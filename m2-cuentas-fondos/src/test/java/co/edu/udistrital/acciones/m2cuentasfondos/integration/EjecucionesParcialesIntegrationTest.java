package co.edu.udistrital.acciones.m2cuentasfondos.integration;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.RoutingKeys;
import co.edu.udistrital.acciones.m2cuentasfondos.application.usecase.ExpirarReservasUseCase;
import co.edu.udistrital.acciones.m2cuentasfondos.support.TokenTestFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class EjecucionesParcialesIntegrationTest extends AbstractIntegrationTest {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private RabbitTemplate rabbit;
    @Autowired private ObjectMapper mapper;
    @Autowired private ExpirarReservasUseCase expirar;

    @Test
    void parcialRestDuplicadoPorRabbitYFinalRabbitNoDuplicanElDebito() throws Exception {
        long usuario = 29901;
        long reserva = preparar(usuario);
        assertThat(confirmar(usuario, reserva, "fill-1", "100.00", false, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        comprobarSaldos(usuario, "700.00", "200.00", "900.00");
        publicar(usuario, "fill-1", "100.00", false, null);
        publicar(usuario, "fill-2", "150.00", true, 2);
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(estado(reserva)).isEqualTo("CONFIRMED"));
        comprobarSaldos(usuario, "750.00", "0.00", "750.00");
        comprobarEjecuciones(reserva, 2);
        assertThat(jdbc.queryForObject("select monto from reserva where id=?", BigDecimal.class, reserva))
                .isEqualByComparingTo("300.00");
        assertThat(jdbc.queryForObject("select monto_consumido from reserva where id=?", BigDecimal.class, reserva))
                .isEqualByComparingTo("250.00");
    }

    @Test
    void finalAdelantadoEsperaElParcialAtrasadoAntesDeLiberar() throws Exception {
        long usuario = 29902;
        long reserva = preparar(usuario);
        assertThat(confirmar(usuario, reserva, "fill-final", "150.00", true, 2).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(estado(reserva)).isEqualTo("PENDING");
        comprobarSaldos(usuario, "700.00", "150.00", "850.00");
        publicar(usuario, "fill-previo", "100.00", false, null);
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(estado(reserva)).isEqualTo("CONFIRMED"));
        comprobarSaldos(usuario, "750.00", "0.00", "750.00");
        assertThat(confirmar(usuario, reserva, "fill-final", "150.00", true, 2).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        comprobarEjecuciones(reserva, 2);
    }

    @Test
    void cancelarTrasParcialLiberaElRemanenteYElTtlNoLiberaLaOrdenAbierta() {
        long usuario = 29903;
        long reserva = preparar(usuario);
        assertThat(confirmar(usuario, reserva, "fill-1", "100.00", false, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        jdbc.update("update reserva set creado_en = now() - interval '10 days' where id=?", reserva);
        expirar.ejecutar();
        assertThat(estado(reserva)).isEqualTo("PENDING");
        comprobarSaldos(usuario, "700.00", "200.00", "900.00");
        ResponseEntity<Map> cancelada = rest.exchange("/api/v1/funds/reservations/" + reserva + "/release",
                HttpMethod.POST, new HttpEntity<>(Map.of("reason", "Cancelada"), headers(usuario)), Map.class);
        assertThat(cancelada.getStatusCode()).isEqualTo(HttpStatus.OK);
        comprobarSaldos(usuario, "900.00", "0.00", "900.00");
        assertThat(confirmar(usuario, reserva, "fill-1", "100.00", false, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        comprobarEjecuciones(reserva, 1);
    }

    @Test
    void fillsConcurrentesSeSerializanYUnDuplicadoIncompatibleEsConflicto() {
        long usuario = 29904;
        long reserva = preparar(usuario);
        var primera = CompletableFuture.supplyAsync(() -> confirmar(usuario, reserva, "fill-1", "100.00", false, null));
        var segunda = CompletableFuture.supplyAsync(() -> confirmar(usuario, reserva, "fill-2", "100.00", false, null));
        assertThat(primera.join().getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(segunda.join().getStatusCode()).isEqualTo(HttpStatus.OK);
        comprobarSaldos(usuario, "700.00", "100.00", "800.00");
        assertThat(confirmar(usuario, reserva, "fill-1", "101.00", false, null).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(confirmar(usuario, reserva, "fill-3", "50.00", true, 3).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        comprobarSaldos(usuario, "750.00", "0.00", "750.00");
        comprobarEjecuciones(reserva, 3);
    }

    private long preparar(long usuario) {
        HttpHeaders admin = headers(1);
        admin.setBearerAuth(TokenTestFactory.token(1, "ADMINISTRADOR"));
        admin.set("Idempotency-Key", "deposito-parcial-" + usuario);
        var deposito = rest.exchange("/api/v1/funds/deposits", HttpMethod.POST,
                new HttpEntity<>(Map.of("userId", usuario, "amount", new BigDecimal("1000.00")), admin), Map.class);
        assertThat(deposito.getStatusCode().is2xxSuccessful()).isTrue();
        HttpHeaders inversor = headers(usuario);
        inversor.set("Idempotency-Key", "reserva-parcial-" + usuario);
        var reserva = rest.exchange("/api/v1/funds/reservations", HttpMethod.POST,
                new HttpEntity<>(Map.of("orderId", "orden-parcial-" + usuario, "userId", usuario,
                        "amount", new BigDecimal("300.00")), inversor), Map.class);
        assertThat(reserva.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return ((Number) reserva.getBody().get("reservationId")).longValue();
    }

    private ResponseEntity<Map> confirmar(long usuario, long reserva, String ejecucion, String monto,
                                          boolean esFinal, Integer esperadas) {
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("executionId", ejecucion);
        payload.put("amount", new BigDecimal(monto));
        payload.put("finalExecution", esFinal);
        if (esperadas != null) payload.put("expectedExecutionCount", esperadas);
        return rest.exchange("/api/v1/funds/reservations/" + reserva + "/confirm", HttpMethod.POST,
                new HttpEntity<>(payload, headers(usuario)), Map.class);
    }

    private void publicar(long usuario, String ejecucion, String monto, boolean esFinal, Integer esperadas)
            throws Exception {
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("orderId", "orden-parcial-" + usuario);
        payload.put("executionId", ejecucion);
        payload.put("totalAmount", new BigDecimal(monto));
        payload.put("side", "BUY");
        payload.put("userId", usuario);
        payload.put("finalExecution", esFinal);
        if (esperadas != null) payload.put("expectedExecutionCount", esperadas);
        String evento = UUID.randomUUID().toString();
        MessageProperties propiedades = new MessageProperties();
        propiedades.setContentType(MediaType.APPLICATION_JSON_VALUE);
        rabbit.send(RoutingKeys.EXCHANGE, RoutingKeys.M5_ORDEN_EJECUTADA,
                new Message(mapper.writeValueAsBytes(Map.of("eventId", evento,
                        "eventType", RoutingKeys.M5_ORDEN_EJECUTADA, "payload", payload)), propiedades));
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(jdbc.queryForObject("select count(*) from evento_procesado where event_id=?",
                        Long.class, evento)).isEqualTo(1));
    }

    private String estado(long reserva) {
        return jdbc.queryForObject("select estado from reserva where id=?", String.class, reserva);
    }

    private void comprobarEjecuciones(long reserva, long cantidad) {
        assertThat(jdbc.queryForObject("select count(*) from reserva_ejecucion where reserva_id=?",
                Long.class, reserva)).isEqualTo(cantidad);
        assertThat(jdbc.queryForObject("select count(*) from movimiento where tipo='DEBITO_COMPRA' "
                        + "and cuenta_id=(select cuenta_id from reserva where id=?)", Long.class, reserva))
                .isEqualTo(cantidad);
    }

    private void comprobarSaldos(long usuario, String disponible, String reservado, String total) {
        Map<String, Object> cuenta = jdbc.queryForMap("select * from cuenta where user_id=?", usuario);
        assertThat((BigDecimal) cuenta.get("saldo_disponible")).isEqualByComparingTo(disponible);
        assertThat((BigDecimal) cuenta.get("saldo_reservado")).isEqualByComparingTo(reservado);
        assertThat((BigDecimal) cuenta.get("saldo_contable")).isEqualByComparingTo(total);
    }

    private HttpHeaders headers(long usuario) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(TokenTestFactory.token(usuario, "INVERSIONISTA"));
        return headers;
    }
}

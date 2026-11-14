package co.edu.udistrital.acciones.m2cuentasfondos.integration;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.RoutingKeys;
import co.edu.udistrital.acciones.m2cuentasfondos.support.TokenTestFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class ConfirmarLiberarReservaIntegrationTest extends AbstractIntegrationTest {

    private static final String COLA = "m2.confirmar.integration";

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void prepararCola() {
        Queue cola = QueueBuilder.durable(COLA).build();
        amqpAdmin.declareQueue(cola);
        amqpAdmin.declareBinding(BindingBuilder.bind(cola)
                .to(new TopicExchange(RoutingKeys.EXCHANGE))
                .with("m2.#"));
        amqpAdmin.purgeQueue(COLA, true);
    }

    @Test
    void confirmarParcialmenteActualizaLosSaldosYRegistraLosMovimientos() throws Exception {
        long userId = 9701L;
        depositar(userId, new BigDecimal("800.00"), "deposito-9701");
        long reservaId = reservar(userId, "orden-9701", new BigDecimal("300.00"),
                "reserva-9701", "trace-reserva-9701");

        ResponseEntity<Map> confirmacion = confirmar(userId, reservaId, "ejecucion-9701",
                new BigDecimal("290.50"), "trace-confirma-9701");

        assertThat(confirmacion.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(confirmacion.getBody().get("status")).isEqualTo("CONFIRMED");
        assertThat(numero(confirmacion.getBody().get("available"))).isEqualByComparingTo("509.50");
        assertThat(numero(confirmacion.getBody().get("reserved"))).isEqualByComparingTo("0.00");
        assertThat(numero(confirmacion.getBody().get("total"))).isEqualByComparingTo("509.50");

        ResponseEntity<Map> saldo = saldo(userId);
        assertThat(numero(saldo.getBody().get("available"))).isEqualByComparingTo("509.50");
        assertThat(numero(saldo.getBody().get("reserved"))).isEqualByComparingTo("0.00");
        assertThat(numero(saldo.getBody().get("total"))).isEqualByComparingTo("509.50");

        Map<String, Object> debito = jdbcTemplate.queryForMap("""
                select monto, saldo_posterior, idempotency_key from movimiento
                where tipo = 'DEBITO_COMPRA' and cuenta_id = (select id from cuenta where user_id = ?)
                """, userId);
        assertThat(numero(debito.get("monto"))).isEqualByComparingTo("290.50");
        assertThat(numero(debito.get("saldo_posterior"))).isEqualByComparingTo("509.50");
        assertThat(debito.get("idempotency_key")).isEqualTo("CNF:" + reservaId);

        Map<String, Object> liberacion = jdbcTemplate.queryForMap("""
                select monto, saldo_posterior, idempotency_key from movimiento
                where tipo = 'LIBERACION' and cuenta_id = (select id from cuenta where user_id = ?)
                """, userId);
        assertThat(numero(liberacion.get("monto"))).isEqualByComparingTo("9.50");
        assertThat(numero(liberacion.get("saldo_posterior"))).isEqualByComparingTo("509.50");
        assertThat(liberacion.get("idempotency_key")).isEqualTo("CNF-LIB:" + reservaId);

        assertThat(jdbcTemplate.queryForObject(
                "select execution_id from reserva where id = ?", String.class, reservaId))
                .isEqualTo("ejecucion-9701");
        assertThat(jdbcTemplate.queryForObject(
                "select estado from reserva where id = ?", String.class, reservaId))
                .isEqualTo("CONFIRMED");

        Map<String, Map<String, Object>> eventos = esperarEventos("trace-confirma-9701",
                Set.of(RoutingKeys.FONDOS_CONFIRMADOS));

        Map<?, ?> confirmados = (Map<?, ?>) eventos.get(RoutingKeys.FONDOS_CONFIRMADOS).get("payload");
        assertThat(((Number) confirmados.get("reservationId")).longValue()).isEqualTo(reservaId);
        assertThat(confirmados.get("executionId")).isEqualTo("ejecucion-9701");
        assertThat(numero(confirmados.get("amount"))).isEqualByComparingTo("290.50");
    }

    @Test
    void confirmarDosVecesConLaMismaEjecucionNoDuplicaMovimientos() {
        long userId = 9702L;
        depositar(userId, new BigDecimal("800.00"), "deposito-9702");
        long reservaId = reservar(userId, "orden-9702", new BigDecimal("300.00"),
                "reserva-9702", "trace-reserva-9702");

        ResponseEntity<Map> primera = confirmar(userId, reservaId, "ejecucion-9702",
                new BigDecimal("290.50"), "trace-confirma-9702-a");
        ResponseEntity<Map> segunda = confirmar(userId, reservaId, "ejecucion-9702",
                new BigDecimal("290.50"), "trace-confirma-9702-b");

        assertThat(primera.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(segunda.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(numero(segunda.getBody().get("available"))).isEqualByComparingTo("509.50");
        assertThat(numero(segunda.getBody().get("reserved"))).isEqualByComparingTo("0.00");
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from movimiento
                where idempotency_key in ('CNF:' || ?, 'CNF-LIB:' || ?)
                """, Long.class, reservaId, reservaId)).isEqualTo(2L);
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from movimiento
                where tipo in ('DEBITO_COMPRA', 'LIBERACION')
                  and cuenta_id = (select id from cuenta where user_id = ?)
                """, Long.class, userId)).isEqualTo(2L);
    }

    @Test
    void confirmarConOtraEjecucionResponde409() {
        long userId = 9703L;
        depositar(userId, new BigDecimal("800.00"), "deposito-9703");
        long reservaId = reservar(userId, "orden-9703", new BigDecimal("300.00"),
                "reserva-9703", "trace-reserva-9703");
        confirmar(userId, reservaId, "ejecucion-9703-a", new BigDecimal("300.00"), "trace-9703-a");

        ResponseEntity<String> respuesta = rest.exchange(
                "/api/v1/funds/reservations/" + reservaId + "/confirm", HttpMethod.POST,
                new HttpEntity<>(Map.of("executionId", "ejecucion-9703-b", "amount", new BigDecimal("300.00")),
                        cabeceras(userId, "INVERSIONISTA")),
                String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(respuesta.getBody()).contains("\"status\":409");
    }

    @Test
    void confirmarUnMontoMayorCubreLaDiferenciaConElDisponible() {
        long userId = 9704L;
        depositar(userId, new BigDecimal("800.00"), "deposito-9704");
        long reservaId = reservar(userId, "orden-9704", new BigDecimal("300.00"),
                "reserva-9704", "trace-reserva-9704");

        ResponseEntity<Map> confirmacion = confirmar(userId, reservaId, "ejecucion-9704",
                new BigDecimal("350.00"), "trace-9704");

        assertThat(confirmacion.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(numero(confirmacion.getBody().get("available"))).isEqualByComparingTo("450.00");
        assertThat(numero(confirmacion.getBody().get("reserved"))).isEqualByComparingTo("0.00");
        assertThat(numero(confirmacion.getBody().get("total"))).isEqualByComparingTo("450.00");
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from movimiento
                where tipo = 'LIBERACION' and cuenta_id = (select id from cuenta where user_id = ?)
                """, Long.class, userId)).isEqualTo(0L);
    }

    @Test
    void confirmarUnMontoMayorSinDisponibleResponde422() {
        long userId = 9705L;
        depositar(userId, new BigDecimal("500.00"), "deposito-9705");
        long reservaId = reservar(userId, "orden-9705", new BigDecimal("500.00"),
                "reserva-9705", "trace-reserva-9705");

        ResponseEntity<String> respuesta = rest.exchange(
                "/api/v1/funds/reservations/" + reservaId + "/confirm", HttpMethod.POST,
                new HttpEntity<>(Map.of("executionId", "ejecucion-9705", "amount", new BigDecimal("500.01")),
                        cabeceras(userId, "INVERSIONISTA")),
                String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(respuesta.getBody()).contains("\"status\":422");
        assertThat(jdbcTemplate.queryForObject(
                "select estado from reserva where id = ?", String.class, reservaId)).isEqualTo("PENDING");
        assertThat(numero(saldo(userId).getBody().get("reserved"))).isEqualByComparingTo("500.00");
    }

    @Test
    void liberarDevuelveElMontoYConfirmarDespuesResponde409() throws Exception {
        long userId = 9706L;
        depositar(userId, new BigDecimal("800.00"), "deposito-9706");
        long reservaId = reservar(userId, "orden-9706", new BigDecimal("300.00"),
                "reserva-9706", "trace-reserva-9706");

        ResponseEntity<Map> liberacion = liberar(userId, reservaId, "OrdenCancelada", "trace-libera-9706");

        assertThat(liberacion.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(liberacion.getBody().get("status")).isEqualTo("RELEASED");
        assertThat(numero(liberacion.getBody().get("available"))).isEqualByComparingTo("800.00");
        assertThat(numero(liberacion.getBody().get("reserved"))).isEqualByComparingTo("0.00");
        assertThat(numero(liberacion.getBody().get("total"))).isEqualByComparingTo("800.00");

        assertThat(jdbcTemplate.queryForObject("""
                select idempotency_key from movimiento
                where tipo = 'LIBERACION' and cuenta_id = (select id from cuenta where user_id = ?)
                """, String.class, userId)).isEqualTo("LIB:" + reservaId);
        assertThat(jdbcTemplate.queryForObject(
                "select motivo_liberacion from reserva where id = ?", String.class, reservaId))
                .isEqualTo("OrdenCancelada");

        ResponseEntity<String> confirmacion = rest.exchange(
                "/api/v1/funds/reservations/" + reservaId + "/confirm", HttpMethod.POST,
                new HttpEntity<>(Map.of("executionId", "ejecucion-9706", "amount", new BigDecimal("300.00")),
                        cabeceras(userId, "INVERSIONISTA")),
                String.class);
        assertThat(confirmacion.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        ResponseEntity<Map> segundaLiberacion = liberar(userId, reservaId, "OrdenCancelada", "trace-libera-9706-b");
        assertThat(segundaLiberacion.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(numero(segundaLiberacion.getBody().get("available"))).isEqualByComparingTo("800.00");
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from movimiento
                where tipo = 'LIBERACION' and cuenta_id = (select id from cuenta where user_id = ?)
                """, Long.class, userId)).isEqualTo(1L);

        Map<String, Map<String, Object>> eventos = esperarEventos("trace-libera-9706",
                Set.of(RoutingKeys.FONDOS_LIBERADOS));
        Map<?, ?> liberados = (Map<?, ?>) eventos.get(RoutingKeys.FONDOS_LIBERADOS).get("payload");
        assertThat(((Number) liberados.get("reservationId")).longValue()).isEqualTo(reservaId);
        assertThat(liberados.get("reason")).isEqualTo("OrdenCancelada");
    }

    @Test
    void unUsuarioAjenoNoPuedeConfirmarNiLiberar() {
        long userId = 9707L;
        depositar(userId, new BigDecimal("800.00"), "deposito-9707");
        long reservaId = reservar(userId, "orden-9707", new BigDecimal("300.00"),
                "reserva-9707", "trace-reserva-9707");

        ResponseEntity<String> confirmacion = rest.exchange(
                "/api/v1/funds/reservations/" + reservaId + "/confirm", HttpMethod.POST,
                new HttpEntity<>(Map.of("executionId", "ejecucion-9707", "amount", new BigDecimal("300.00")),
                        cabeceras(9708L, "INVERSIONISTA")),
                String.class);
        ResponseEntity<String> liberacion = rest.exchange(
                "/api/v1/funds/reservations/" + reservaId + "/release", HttpMethod.POST,
                new HttpEntity<>(Map.of("reason", "OrdenCancelada"), cabeceras(9708L, "INVERSIONISTA")),
                String.class);

        assertThat(confirmacion.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(liberacion.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(jdbcTemplate.queryForObject(
                "select estado from reserva where id = ?", String.class, reservaId)).isEqualTo("PENDING");
    }

    @Test
    void unaReservaInexistenteResponde404() {
        long userId = 9709L;
        depositar(userId, new BigDecimal("800.00"), "deposito-9709");

        ResponseEntity<String> confirmacion = rest.exchange(
                "/api/v1/funds/reservations/999999/confirm", HttpMethod.POST,
                new HttpEntity<>(Map.of("executionId", "ejecucion-9709", "amount", new BigDecimal("10.00")),
                        cabeceras(userId, "INVERSIONISTA")),
                String.class);
        ResponseEntity<String> liberacion = rest.exchange(
                "/api/v1/funds/reservations/999999/release", HttpMethod.POST,
                new HttpEntity<>(Map.of("reason", "OrdenCancelada"), cabeceras(userId, "INVERSIONISTA")),
                String.class);

        assertThat(confirmacion.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(liberacion.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private ResponseEntity<Map> depositar(long userId, BigDecimal monto, String clave) {
        return rest.exchange("/api/v1/funds/deposits", HttpMethod.POST,
                new HttpEntity<>(Map.of("userId", userId, "amount", monto),
                        cabecerasConClave(1L, "ADMINISTRADOR", clave)), Map.class);
    }

    private long reservar(long userId, String orden, BigDecimal monto, String clave, String correlacion) {
        HttpHeaders headers = cabecerasConClave(userId, "INVERSIONISTA", clave);
        headers.set("X-Correlation-ID", correlacion);
        ResponseEntity<Map> respuesta = rest.exchange("/api/v1/funds/reservations", HttpMethod.POST,
                new HttpEntity<>(Map.of("orderId", orden, "userId", userId, "amount", monto), headers), Map.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return ((Number) respuesta.getBody().get("reservationId")).longValue();
    }

    private ResponseEntity<Map> confirmar(long userId, long reservaId, String executionId, BigDecimal monto,
                                          String correlacion) {
        HttpHeaders headers = cabeceras(userId, "INVERSIONISTA");
        headers.set("X-Correlation-ID", correlacion);
        return rest.exchange("/api/v1/funds/reservations/" + reservaId + "/confirm", HttpMethod.POST,
                new HttpEntity<>(Map.of("executionId", executionId, "amount", monto), headers), Map.class);
    }

    private ResponseEntity<Map> liberar(long userId, long reservaId, String motivo, String correlacion) {
        HttpHeaders headers = cabeceras(userId, "INVERSIONISTA");
        headers.set("X-Correlation-ID", correlacion);
        return rest.exchange("/api/v1/funds/reservations/" + reservaId + "/release", HttpMethod.POST,
                new HttpEntity<>(Map.of("reason", motivo), headers), Map.class);
    }

    private ResponseEntity<Map> saldo(long userId) {
        return rest.exchange("/api/v1/accounts/" + userId + "/balance", HttpMethod.GET,
                new HttpEntity<>(cabeceras(userId, "INVERSIONISTA")), Map.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Object>> esperarEventos(String correlacion, Set<String> tipos) throws Exception {
        Map<String, Map<String, Object>> encontrados = new HashMap<>();
        await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(200)).until(() -> {
            Message mensaje;
            while ((mensaje = rabbitTemplate.receive(COLA, 200)) != null) {
                Map<?, ?> sobre = objectMapper.readValue(mensaje.getBody(), Map.class);
                if (correlacion.equals(sobre.get("correlationId"))) {
                    encontrados.put((String) sobre.get("eventType"), (Map<String, Object>) sobre);
                }
            }
            return encontrados.keySet().containsAll(tipos);
        });
        return encontrados;
    }

    private HttpHeaders cabeceras(long userId, String... roles) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(TokenTestFactory.token(userId, roles));
        return headers;
    }

    private HttpHeaders cabecerasConClave(long userId, String rol, String clave) {
        HttpHeaders headers = cabeceras(userId, rol);
        headers.set("Idempotency-Key", clave);
        return headers;
    }

    private BigDecimal numero(Object valor) {
        return new BigDecimal(valor.toString());
    }
}

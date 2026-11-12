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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class ReservasIntegrationTest extends AbstractIntegrationTest {

    private static final String COLA = "m2.reservas.integration";

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
    void reservarMueveElDineroSinTocarElContableYPublicaEventos() throws Exception {
        long userId = 9601L;
        depositar(userId, new BigDecimal("500.00"), "deposito-9601");

        ResponseEntity<Map> reserva = reservar(userId, "orden-9601", new BigDecimal("100.00"),
                "reserva-9601", "trace-reserva-9601");

        assertThat(reserva.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(reserva.getBody().get("status")).isEqualTo("PENDING");
        assertThat(numero(reserva.getBody().get("amount"))).isEqualByComparingTo("100.00");
        assertThat(numero(reserva.getBody().get("available"))).isEqualByComparingTo("400.00");
        assertThat(numero(reserva.getBody().get("reserved"))).isEqualByComparingTo("100.00");
        assertThat(numero(reserva.getBody().get("total"))).isEqualByComparingTo("500.00");

        long reservaId = ((Number) reserva.getBody().get("reservationId")).longValue();

        ResponseEntity<Map> consulta = rest.exchange("/api/v1/funds/reservations/" + reservaId,
                HttpMethod.GET, new HttpEntity<>(cabeceras(userId, "INVERSIONISTA")), Map.class);

        assertThat(consulta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(consulta.getBody().get("status")).isEqualTo("PENDING");
        assertThat(numero(consulta.getBody().get("reserved"))).isEqualByComparingTo("100.00");

        ResponseEntity<Map> saldo = rest.exchange("/api/v1/accounts/" + userId + "/balance",
                HttpMethod.GET, new HttpEntity<>(cabeceras(userId, "INVERSIONISTA")), Map.class);

        assertThat(numero(saldo.getBody().get("available"))).isEqualByComparingTo("400.00");
        assertThat(numero(saldo.getBody().get("reserved"))).isEqualByComparingTo("100.00");
        assertThat(numero(saldo.getBody().get("total"))).isEqualByComparingTo("500.00");
        assertThat(jdbcTemplate.queryForObject("""
                select monto from movimiento
                where tipo = 'RESERVA' and cuenta_id = (select id from cuenta where user_id = ?)
                """, BigDecimal.class, userId)).isEqualByComparingTo("100.00");
        assertThat(jdbcTemplate.queryForObject("""
                select saldo_posterior from movimiento
                where tipo = 'RESERVA' and cuenta_id = (select id from cuenta where user_id = ?)
                """, BigDecimal.class, userId)).isEqualByComparingTo("400.00");

        Map<String, Map<String, Object>> eventos = esperarEventos("trace-reserva-9601",
                Set.of(RoutingKeys.FONDOS_RESERVADOS, RoutingKeys.MOVIMIENTO_CREADO));

        Map<?, ?> reservados = (Map<?, ?>) eventos.get(RoutingKeys.FONDOS_RESERVADOS).get("payload");
        assertThat(((Number) reservados.get("reservationId")).longValue()).isEqualTo(reservaId);
        assertThat(reservados.get("orderId")).isEqualTo("orden-9601");
        assertThat(((Number) reservados.get("userId")).longValue()).isEqualTo(userId);
        assertThat(numero(reservados.get("amount"))).isEqualByComparingTo("100.00");

        Map<?, ?> movimiento = (Map<?, ?>) eventos.get(RoutingKeys.MOVIMIENTO_CREADO).get("payload");
        assertThat(movimiento.get("type")).isEqualTo("RESERVA");
        assertThat(numero(movimiento.get("amount"))).isEqualByComparingTo("100.00");
        assertThat(numero(movimiento.get("balance"))).isEqualByComparingTo("400.00");
    }

    @Test
    void repetirLaMismaClaveNoDuplicaLaReserva() {
        long userId = 9602L;
        depositar(userId, new BigDecimal("500.00"), "deposito-9602");

        ResponseEntity<Map> primero = reservar(userId, "orden-9602", new BigDecimal("100.00"),
                "reserva-9602", "trace-9602");
        ResponseEntity<Map> segundo = reservar(userId, "orden-9602", new BigDecimal("100.00"),
                "reserva-9602", "trace-9602");

        assertThat(primero.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(segundo.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(segundo.getBody().get("reservationId")).isEqualTo(primero.getBody().get("reservationId"));
        assertThat(numero(segundo.getBody().get("available"))).isEqualByComparingTo("400.00");
        assertThat(numero(segundo.getBody().get("reserved"))).isEqualByComparingTo("100.00");
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from reserva where cuenta_id = (select id from cuenta where user_id = ?)",
                Long.class, userId)).isEqualTo(1L);
    }

    @Test
    void unOrderIdRepetidoResponde409() {
        long userId = 9603L;
        depositar(userId, new BigDecimal("500.00"), "deposito-9603");
        reservar(userId, "orden-9603", new BigDecimal("100.00"), "reserva-9603-a", "trace-9603-a");

        ResponseEntity<String> respuesta = rest.exchange("/api/v1/funds/reservations", HttpMethod.POST,
                new HttpEntity<>("{\"orderId\":\"orden-9603\",\"userId\":" + userId + ",\"amount\":50.00}",
                        cabecerasConClave(userId, "INVERSIONISTA", "reserva-9603-b")),
                String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(respuesta.getBody()).contains("\"status\":409");
    }

    @Test
    void sinFondosResponde422() {
        long userId = 9604L;
        depositar(userId, new BigDecimal("50.00"), "deposito-9604");

        ResponseEntity<String> respuesta = rest.exchange("/api/v1/funds/reservations", HttpMethod.POST,
                new HttpEntity<>("{\"orderId\":\"orden-9604\",\"userId\":" + userId + ",\"amount\":75.00}",
                        cabecerasConClave(userId, "INVERSIONISTA", "reserva-9604")),
                String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(respuesta.getBody()).contains("\"status\":422");
    }

    @Test
    void unUsuarioAjenoNoPuedeReservar() {
        long userId = 9605L;
        depositar(userId, new BigDecimal("500.00"), "deposito-9605");

        ResponseEntity<String> respuesta = rest.exchange("/api/v1/funds/reservations", HttpMethod.POST,
                new HttpEntity<>("{\"orderId\":\"orden-9605\",\"userId\":" + userId + ",\"amount\":10.00}",
                        cabecerasConClave(9606L, "INVERSIONISTA", "reserva-9605")),
                String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(respuesta.getBody()).contains("\"status\":403");
    }

    @Test
    void unaReservaAjenaNoSePuedeConsultar() {
        long userId = 9607L;
        depositar(userId, new BigDecimal("500.00"), "deposito-9607");
        ResponseEntity<Map> reserva = reservar(userId, "orden-9607", new BigDecimal("100.00"),
                "reserva-9607", "trace-9607");
        long reservaId = ((Number) reserva.getBody().get("reservationId")).longValue();

        ResponseEntity<String> respuesta = rest.exchange("/api/v1/funds/reservations/" + reservaId,
                HttpMethod.GET, new HttpEntity<>(cabeceras(9608L, "INVERSIONISTA")), String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(respuesta.getBody()).contains("\"status\":403");
    }

    @Test
    void laConcurrenciaNoSobregiraLaCuenta() throws Exception {
        long userId = 9609L;
        depositar(userId, new BigDecimal("500.00"), "deposito-9609");

        int hilos = 10;
        ExecutorService ejecutor = Executors.newFixedThreadPool(hilos);
        CountDownLatch listos = new CountDownLatch(hilos);
        CountDownLatch disparo = new CountDownLatch(1);
        List<Future<HttpStatus>> futuros = new ArrayList<>();

        try {
            for (int i = 0; i < hilos; i++) {
                String orden = "orden-concurrente-9609-" + i;
                String clave = "reserva-concurrente-9609-" + i;
                futuros.add(ejecutor.submit(() -> {
                    listos.countDown();
                    disparo.await(30, TimeUnit.SECONDS);
                    return HttpStatus.valueOf(reservar(userId, orden, new BigDecimal("100.00"),
                            clave, "trace-concurrente-9609").getStatusCode().value());
                }));
            }
            assertThat(listos.await(30, TimeUnit.SECONDS)).isTrue();
            disparo.countDown();

            Map<Integer, Integer> conteo = new HashMap<>();
            for (Future<HttpStatus> futuro : futuros) {
                HttpStatus estado = futuro.get(60, TimeUnit.SECONDS);
                conteo.merge(estado.value(), 1, Integer::sum);
            }

            assertThat(conteo.get(HttpStatus.CREATED.value())).isEqualTo(5);
            assertThat(conteo.get(HttpStatus.UNPROCESSABLE_ENTITY.value())).isEqualTo(5);
        } finally {
            ejecutor.shutdownNow();
        }

        ResponseEntity<Map> saldo = rest.exchange("/api/v1/accounts/" + userId + "/balance",
                HttpMethod.GET, new HttpEntity<>(cabeceras(userId, "INVERSIONISTA")), Map.class);

        assertThat(numero(saldo.getBody().get("available"))).isEqualByComparingTo("0.00");
        assertThat(numero(saldo.getBody().get("reserved"))).isEqualByComparingTo("500.00");
        assertThat(numero(saldo.getBody().get("total"))).isEqualByComparingTo("500.00");
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from reserva where cuenta_id = (select id from cuenta where user_id = ?)",
                Long.class, userId)).isEqualTo(5L);
    }

    private ResponseEntity<Map> depositar(long userId, BigDecimal monto, String clave) {
        return rest.exchange("/api/v1/funds/deposits", HttpMethod.POST,
                new HttpEntity<>(Map.of("userId", userId, "amount", monto),
                        cabecerasConClave(1L, "ADMINISTRADOR", clave)), Map.class);
    }

    private ResponseEntity<Map> reservar(long userId, String orden, BigDecimal monto, String clave,
                                         String correlacion) {
        HttpHeaders headers = cabecerasConClave(userId, "INVERSIONISTA", clave);
        headers.set("X-Correlation-ID", correlacion);
        return rest.exchange("/api/v1/funds/reservations", HttpMethod.POST,
                new HttpEntity<>(Map.of("orderId", orden, "userId", userId, "amount", monto), headers), Map.class);
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

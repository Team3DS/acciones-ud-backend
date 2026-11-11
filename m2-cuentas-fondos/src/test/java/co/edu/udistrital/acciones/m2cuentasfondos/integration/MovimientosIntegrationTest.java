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
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class MovimientosIntegrationTest extends AbstractIntegrationTest {

    private static final String COLA = "m2.movimientos.integration";
    private static final String CORRELACION_RETIRO = "trace-retiro-9401";

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
    void elDuenoRetiraYListaSusMovimientos() throws Exception {
        long userId = 9401L;
        depositar(userId, new BigDecimal("1000.00"), "deposito-9401");

        ResponseEntity<Map> retiro = retirar(userId, new BigDecimal("200.00"), "retiro-9401",
                CORRELACION_RETIRO);

        assertThat(retiro.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(numero(retiro.getBody().get("amount"))).isEqualByComparingTo("200.00");
        assertThat(numero(retiro.getBody().get("available"))).isEqualByComparingTo("800.00");
        assertThat(numero(retiro.getBody().get("total"))).isEqualByComparingTo("800.00");

        ResponseEntity<Map> listado = rest.exchange(
                uriMovimientos(userId), HttpMethod.GET,
                new HttpEntity<>(cabeceras(userId, "INVERSIONISTA")), Map.class);

        assertThat(listado.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> contenido = contenido(listado);
        assertThat(contenido).hasSize(2);
        assertThat(contenido.get(0).get("type")).isEqualTo("RETIRO");
        assertThat(numero(contenido.get(0).get("balanceAfter"))).isEqualByComparingTo("800.00");
        assertThat(contenido.get(1).get("type")).isEqualTo("DEPOSITO");
        assertThat(numero(contenido.get(1).get("balanceAfter"))).isEqualByComparingTo("1000.00");

        Map<String, Object> pagina = pagina(listado);
        assertThat(((Number) pagina.get("totalElements")).longValue()).isEqualTo(2);
        assertThat(((Number) pagina.get("number")).intValue()).isZero();
        assertThat(((Number) pagina.get("size")).intValue()).isEqualTo(20);

        assertThat(contenido(filtrado(userId, "filter[tipo]", "RETIRO"))).singleElement()
                .satisfies(movimiento -> {
                    assertThat(movimiento.get("type")).isEqualTo("RETIRO");
                    assertThat(numero(movimiento.get("balanceAfter"))).isEqualByComparingTo("800.00");
                });
        assertThat(contenido(filtrado(userId,
                "filter[desde]", Instant.now().minusSeconds(120).toString(),
                "filter[hasta]", Instant.now().plusSeconds(120).toString()))).hasSize(2);
        assertThat(contenido(filtrado(userId,
                "filter[desde]", Instant.now().plusSeconds(3600).toString()))).isEmpty();

        Map<String, Object> sobre = esperarMensaje(CORRELACION_RETIRO);
        assertThat(sobre.get("eventType")).isEqualTo(RoutingKeys.MOVIMIENTO_CREADO);
        Map<?, ?> payload = (Map<?, ?>) sobre.get("payload");
        assertThat(payload.get("type")).isEqualTo("RETIRO");
        assertThat(numero(payload.get("amount"))).isEqualByComparingTo("200.00");
        assertThat(numero(payload.get("balance"))).isEqualByComparingTo("800.00");
    }

    @Test
    void repetirLaMismaClaveNoDuplicaElRetiro() {
        long userId = 9402L;
        depositar(userId, new BigDecimal("500.00"), "deposito-9402");

        ResponseEntity<Map> primero = retirar(userId, new BigDecimal("100.00"), "retiro-9402", "demo-9402");
        ResponseEntity<Map> segundo = retirar(userId, new BigDecimal("100.00"), "retiro-9402", "demo-9402");

        assertThat(primero.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(segundo.getBody().get("movementId")).isEqualTo(primero.getBody().get("movementId"));
        assertThat(numero(segundo.getBody().get("available"))).isEqualByComparingTo("400.00");
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from movimiento where cuenta_id = (select id from cuenta where user_id = ?)",
                Long.class, userId)).isEqualTo(2L);
    }

    @Test
    void retirarSinFondosResponde422() {
        long userId = 9403L;
        depositar(userId, new BigDecimal("50.00"), "deposito-9403");

        ResponseEntity<String> respuesta = rest.exchange("/api/v1/funds/withdrawals", HttpMethod.POST,
                new HttpEntity<>("{\"userId\":" + userId + ",\"amount\":75.00}",
                        cabecerasConClave(userId, "INVERSIONISTA", "retiro-9403")),
                String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(respuesta.getBody()).contains("\"status\":422");
    }

    @Test
    void unUsuarioAjenoNoPuedeRetirar() {
        long userId = 9404L;
        depositar(userId, new BigDecimal("100.00"), "deposito-9404");

        ResponseEntity<String> respuesta = rest.exchange("/api/v1/funds/withdrawals", HttpMethod.POST,
                new HttpEntity<>("{\"userId\":" + userId + ",\"amount\":10.00}",
                        cabecerasConClave(9405L, "INVERSIONISTA", "retiro-9404")),
                String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(respuesta.getBody()).contains("\"status\":403");
    }

    private ResponseEntity<Map> depositar(long userId, BigDecimal monto, String clave) {
        HttpHeaders headers = cabecerasConClave(1L, "ADMINISTRADOR", clave);
        return rest.exchange("/api/v1/funds/deposits", HttpMethod.POST,
                new HttpEntity<>(Map.of("userId", userId, "amount", monto), headers), Map.class);
    }

    private ResponseEntity<Map> retirar(long userId, BigDecimal monto, String clave, String correlacion) {
        HttpHeaders headers = cabecerasConClave(userId, "INVERSIONISTA", clave);
        headers.set("X-Correlation-ID", correlacion);
        return rest.exchange("/api/v1/funds/withdrawals", HttpMethod.POST,
                new HttpEntity<>(Map.of("userId", userId, "amount", monto), headers), Map.class);
    }

    private ResponseEntity<Map> filtrado(long userId, String... parametros) {
        return rest.exchange(uriMovimientos(userId, parametros), HttpMethod.GET,
                new HttpEntity<>(cabeceras(userId, "INVERSIONISTA")), Map.class);
    }

    private URI uriMovimientos(long userId, String... parametros) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/api/v1/accounts/{userId}/movements");
        for (int i = 0; i < parametros.length; i += 2) {
            builder.queryParam(parametros[i], parametros[i + 1]);
        }
        return builder.encode().buildAndExpand(userId).toUri();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> contenido(ResponseEntity<Map> respuesta) {
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (List<Map<String, Object>>) respuesta.getBody().get("content");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> pagina(ResponseEntity<Map> respuesta) {
        return (Map<String, Object>) respuesta.getBody().get("page");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> esperarMensaje(String correlacion) throws Exception {
        AtomicReference<Message> recibido = new AtomicReference<>();
        await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(200)).until(() -> {
            Message mensaje = rabbitTemplate.receive(COLA, 1000);
            if (mensaje == null) {
                return false;
            }
            Map<?, ?> sobre = objectMapper.readValue(mensaje.getBody(), Map.class);
            if (correlacion.equals(sobre.get("correlationId"))) {
                recibido.set(mensaje);
                return true;
            }
            return false;
        });
        return (Map<String, Object>) objectMapper.readValue(recibido.get().getBody(), Map.class);
    }

    private HttpHeaders cabeceras(Long userId, String... roles) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(TokenTestFactory.token(userId, roles));
        return headers;
    }

    private HttpHeaders cabecerasConClave(Long userId, String rol, String clave) {
        HttpHeaders headers = cabeceras(userId, rol);
        headers.set("Idempotency-Key", clave);
        return headers;
    }

    private BigDecimal numero(Object valor) {
        return new BigDecimal(valor.toString());
    }
}

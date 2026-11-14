package co.edu.udistrital.acciones.m2cuentasfondos.integration;

import co.edu.udistrital.acciones.m2cuentasfondos.support.TokenTestFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class CuentasIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void unAdministradorDepositaYElDuenoConsultaSuSaldo() {
        long userId = 9101L;

        ResponseEntity<Map> deposito = depositar(1L, "ADMINISTRADOR", userId,
                new BigDecimal("1000.00"), "deposito-9101");

        assertThat(deposito.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(numero(deposito.getBody().get("available"))).isEqualByComparingTo("1000.00");
        assertThat(numero(deposito.getBody().get("reserved"))).isEqualByComparingTo("0.00");
        assertThat(numero(deposito.getBody().get("total"))).isEqualByComparingTo("1000.00");
        assertThat(deposito.getBody().get("currency")).isEqualTo("USD");

        ResponseEntity<Map> saldo = rest.exchange("/api/v1/accounts/" + userId + "/balance",
                HttpMethod.GET, new HttpEntity<>(cabeceras(userId, "INVERSIONISTA")), Map.class);

        assertThat(saldo.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(numero(saldo.getBody().get("available"))).isEqualByComparingTo("1000.00");
        assertThat(numero(saldo.getBody().get("reserved"))).isEqualByComparingTo("0.00");
        assertThat(numero(saldo.getBody().get("total"))).isEqualByComparingTo("1000.00");
    }

    @Test
    void repetirLaMismaClaveNoDuplicaElDeposito() {
        long userId = 9102L;
        String clave = "deposito-repetido-9102";

        ResponseEntity<Map> primero = depositar(1L, "ADMINISTRADOR", userId,
                new BigDecimal("500.00"), clave);
        ResponseEntity<Map> segundo = depositar(1L, "ADMINISTRADOR", userId,
                new BigDecimal("500.00"), clave);

        assertThat(primero.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(segundo.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(segundo.getBody().get("movementId")).isEqualTo(primero.getBody().get("movementId"));

        ResponseEntity<Map> saldo = rest.exchange("/api/v1/accounts/" + userId + "/balance",
                HttpMethod.GET, new HttpEntity<>(cabeceras(userId, "INVERSIONISTA")), Map.class);

        assertThat(numero(saldo.getBody().get("available"))).isEqualByComparingTo("500.00");
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from movimiento where cuenta_id = (select id from cuenta where user_id = ?)",
                Long.class, userId)).isEqualTo(1L);
    }

    @Test
    void laMismaClaveConOtroMontoResponde409() {
        long userId = 9103L;
        String clave = "deposito-conflicto-9103";

        depositar(1L, "ADMINISTRADOR", userId, new BigDecimal("100.00"), clave);
        HttpHeaders headers = cabeceras(1L, "ADMINISTRADOR");
        headers.set("Idempotency-Key", clave);
        ResponseEntity<String> conflicto = rest.exchange("/api/v1/funds/deposits", HttpMethod.POST,
                new HttpEntity<>("{\"userId\":" + userId + ",\"amount\":200.00}", headers),
                String.class);

        assertThat(conflicto.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(conflicto.getHeaders().getContentType())
                .isNotNull()
                .matches(tipo -> tipo.isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        assertThat(conflicto.getBody()).contains("\"status\":409");
    }

    @Test
    void unInversionistaNoPuedeDepositar() {
        ResponseEntity<String> respuesta = rest.exchange("/api/v1/funds/deposits", HttpMethod.POST,
                new HttpEntity<>("{\"userId\":9104,\"amount\":100.00}", cabeceras(9104L, "INVERSIONISTA")),
                String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(respuesta.getBody()).contains("\"status\":403");
    }

    @Test
    void laConcurrenciaCreaUnaSolaCuenta() throws Exception {
        long userId = 9105L;
        int hilos = 10;
        ExecutorService ejecutor = Executors.newFixedThreadPool(hilos);
        CountDownLatch listos = new CountDownLatch(hilos);
        CountDownLatch disparo = new CountDownLatch(1);
        List<Future<ResponseEntity<Map>>> futuros = new ArrayList<>();

        try {
            for (int i = 0; i < hilos; i++) {
                String clave = "concurrente-9105-" + i;
                futuros.add(ejecutor.submit(() -> {
                    listos.countDown();
                    disparo.await(30, TimeUnit.SECONDS);
                    return depositar(1L, "ADMINISTRADOR", userId, new BigDecimal("100.00"), clave);
                }));
            }
            assertThat(listos.await(30, TimeUnit.SECONDS)).isTrue();
            disparo.countDown();

            for (Future<ResponseEntity<Map>> futuro : futuros) {
                assertThat(futuro.get(60, TimeUnit.SECONDS).getStatusCode()).isEqualTo(HttpStatus.CREATED);
            }
        } finally {
            ejecutor.shutdownNow();
        }

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from cuenta where user_id = ?", Long.class, userId)).isEqualTo(1L);

        ResponseEntity<Map> saldo = rest.exchange("/api/v1/accounts/" + userId + "/balance",
                HttpMethod.GET, new HttpEntity<>(cabeceras(userId, "INVERSIONISTA")), Map.class);

        assertThat(numero(saldo.getBody().get("available"))).isEqualByComparingTo("1000.00");
    }

    private ResponseEntity<Map> depositar(Long actorId, String rol, Long userId, BigDecimal monto, String clave) {
        HttpHeaders headers = cabeceras(actorId, rol);
        headers.set("Idempotency-Key", clave);
        return rest.exchange("/api/v1/funds/deposits", HttpMethod.POST,
                new HttpEntity<>(Map.of("userId", userId, "amount", monto), headers), Map.class);
    }

    private HttpHeaders cabeceras(Long userId, String... roles) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(TokenTestFactory.token(userId, roles));
        return headers;
    }

    private BigDecimal numero(Object valor) {
        return new BigDecimal(valor.toString());
    }
}

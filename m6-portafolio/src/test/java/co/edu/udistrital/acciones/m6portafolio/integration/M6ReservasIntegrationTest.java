package co.edu.udistrital.acciones.m6portafolio.integration;

import co.edu.udistrital.acciones.m6portafolio.support.TokenTestFactory;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class M6ReservasIntegrationTest extends AbstractIntegrationTest {

    private static final Instant EJECUTADO_EN = Instant.parse("2026-02-02T10:00:00Z");
    private static final String CLAVE_IDEMPOTENCIA = "Idempotency-Key";

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void flywayAplicaLaTablaDeReservas() {
        Integer migraciones = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where version = '3' and success = true",
                Integer.class);
        Integer tablas = jdbc.queryForObject("""
                select count(*) from information_schema.tables
                where table_schema = 'public' and table_name = 'reserva_posicion'
                """, Integer.class);

        assertThat(migraciones).isEqualTo(1);
        assertThat(tablas).isEqualTo(1);
    }

    @Test
    void reservarReduceElDisponibleYLiberarLoRestaura() {
        long userId = 5201L;
        String token = TokenTestFactory.token(userId, "INVERSIONISTA");
        comprar(token, userId, 5201L, "EXE-5201", "AAPL", "20", "185.00");

        ResponseEntity<Map> reserva = reservar(token, userId, 520101L, "AAPL", "10", "clave-5201", null);
        assertThat(reserva.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(reserva.getBody().get("status")).isEqualTo("ACTIVA");
        assertThat(numero(reserva.getBody(), "quantity")).isEqualTo(10.0);
        assertThat(numero(reserva.getBody(), "executedQuantity")).isZero();
        long reservaId = idReserva(reserva);

        Map<?, ?> posicion = posicion(token, userId, "AAPL");
        assertThat(numero(posicion, "quantity")).isEqualTo(20.0);
        assertThat(numero(posicion, "reservedQuantity")).isEqualTo(10.0);
        assertThat(numero(posicion, "availableQuantity")).isEqualTo(10.0);

        ResponseEntity<Map> liberada = liberar(token, reservaId, "cancelada");
        assertThat(liberada.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(liberada.getBody().get("status")).isEqualTo("LIBERADA");
        assertThat(liberada.getBody().get("reason")).isEqualTo("cancelada");

        Map<?, ?> despues = posicion(token, userId, "AAPL");
        assertThat(numero(despues, "quantity")).isEqualTo(20.0);
        assertThat(numero(despues, "reservedQuantity")).isZero();
        assertThat(numero(despues, "availableQuantity")).isEqualTo(20.0);

        assertThat(movimientosConClave("RES:" + reservaId)).isEqualTo(1);
        assertThat(movimientosConClave("LIB:" + reservaId)).isEqualTo(1);
    }

    @Test
    void liberarDosVecesResponde200SinDuplicarElMovimiento() {
        long userId = 5202L;
        String token = TokenTestFactory.token(userId, "INVERSIONISTA");
        comprar(token, userId, 5202L, "EXE-5202", "TSLA", "10", "200.00");
        ResponseEntity<Map> reserva = reservar(token, userId, 520201L, "TSLA", "5", "clave-5202", null);
        long reservaId = idReserva(reserva);

        ResponseEntity<Map> primera = liberar(token, reservaId, "cancelada");
        ResponseEntity<Map> segunda = liberar(token, reservaId, "otra vez");

        assertThat(primera.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(segunda.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(segunda.getBody().get("status")).isEqualTo("LIBERADA");
        assertThat(segunda.getBody().get("reason")).isEqualTo("cancelada");
        assertThat(movimientosConClave("LIB:" + reservaId)).isEqualTo(1);
        assertThat(numero(posicion(token, userId, "TSLA"), "availableQuantity")).isEqualTo(10.0);
    }

    @Test
    void laClaveRepetidaDevuelveLaMismaReserva() {
        long userId = 5203L;
        String token = TokenTestFactory.token(userId, "INVERSIONISTA");
        comprar(token, userId, 5203L, "EXE-5203", "MSFT", "20", "150.00");

        ResponseEntity<Map> primera = reservar(token, userId, 520301L, "MSFT", "5", "clave-5203", null);
        ResponseEntity<Map> repetida = reservar(token, userId, 520301L, "MSFT", "5", "clave-5203", null);

        assertThat(primera.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(repetida.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(idReserva(repetida)).isEqualTo(idReserva(primera));
        assertThat(numero(posicion(token, userId, "MSFT"), "reservedQuantity")).isEqualTo(5.0);
        assertThat(movimientosConClave("RES:" + idReserva(primera))).isEqualTo(1);
    }

    @Test
    void laClaveRepetidaConOtrosDatosResponde409() {
        long userId = 5208L;
        String token = TokenTestFactory.token(userId, "INVERSIONISTA");
        comprar(token, userId, 5208L, "EXE-5208", "MSFT", "20", "150.00");
        reservar(token, userId, 520801L, "MSFT", "5", "clave-5208", null);

        ResponseEntity<Map> respuesta = reservar(token, userId, 520801L, "MSFT", "6", "clave-5208", null);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(numero(posicion(token, userId, "MSFT"), "reservedQuantity")).isEqualTo(5.0);
    }

    @Test
    void unaOrdenDuplicadaConOtraClaveResponde409() {
        long userId = 5209L;
        String token = TokenTestFactory.token(userId, "INVERSIONISTA");
        comprar(token, userId, 5209L, "EXE-5209", "MSFT", "20", "150.00");
        reservar(token, userId, 520901L, "MSFT", "5", "clave-5209", null);

        ResponseEntity<Map> respuesta = reservar(token, userId, 520901L, "MSFT", "5",
                "otra-clave-5209", null);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(numero(posicion(token, userId, "MSFT"), "reservedQuantity")).isEqualTo(5.0);
    }

    @Test
    void sinPosicionOSinDisponibleResponde422() {
        long userId = 5204L;
        String token = TokenTestFactory.token(userId, "INVERSIONISTA");

        ResponseEntity<Map> sinPosicion = reservar(token, userId, "ORD-5204-A", "AAPL", "10",
                "clave-5204-a", null);
        assertThat(sinPosicion.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        comprar(token, userId, "ORD-5204", "EXE-5204", "AAPL", "5", "180.00");
        ResponseEntity<Map> insuficiente = reservar(token, userId, "ORD-5204-B", "AAPL", "10",
                "clave-5204-b", null);

        assertThat(insuficiente.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(numero(posicion(token, userId, "AAPL"), "reservedQuantity")).isZero();
    }

    @Test
    void unUsuarioAjenoResponde403YElSistemaPuedeReservar() {
        long userId = 5205L;
        String tokenDueno = TokenTestFactory.token(userId, "INVERSIONISTA");
        comprar(tokenDueno, userId, "ORD-5205", "EXE-5205", "NVDA", "10", "500.00");

        String tokenAjeno = TokenTestFactory.token(5299L, "INVERSIONISTA");
        ResponseEntity<Map> ajena = reservar(tokenAjeno, userId, "ORD-5205-A", "NVDA", "2",
                "clave-5205-a", null);
        assertThat(ajena.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(numero(posicion(tokenDueno, userId, "NVDA"), "reservedQuantity")).isZero();

        String tokenSistema = TokenTestFactory.sistema();
        ResponseEntity<Map> delSistema = reservar(tokenSistema, userId, "ORD-5205-B", "NVDA", "3",
                "clave-5205-b", null);

        assertThat(delSistema.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(numero(posicion(tokenDueno, userId, "NVDA"), "reservedQuantity")).isEqualTo(3.0);
    }

    @Test
    void lasClavesDelHeaderYDelCuerpoDebenCoincidir() {
        long userId = 5207L;
        String token = TokenTestFactory.token(userId, "INVERSIONISTA");
        comprar(token, userId, "ORD-5207", "EXE-5207", "AMD", "10", "120.00");

        ResponseEntity<Map> soloHeader = reservar(token, userId, "ORD-5207-A", "AMD", "1",
                null, "clave-5207-a");
        ResponseEntity<Map> iguales = reservar(token, userId, "ORD-5207-B", "AMD", "1",
                "clave-5207-b", "clave-5207-b");
        ResponseEntity<Map> distintas = reservar(token, userId, "ORD-5207-C", "AMD", "1",
                "clave-5207-c", "otra-clave");
        ResponseEntity<Map> sinClave = reservar(token, userId, "ORD-5207-D", "AMD", "1", null, null);
        ResponseEntity<Map> claveLarga = reservar(token, userId, "ORD-5207-E", "AMD", "1",
                null, "x".repeat(65));

        assertThat(soloHeader.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(iguales.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(distintas.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(sinClave.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(claveLarga.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void lasReservasConcurrentesNoSuperanElDisponible() throws Exception {
        long userId = 5206L;
        String token = TokenTestFactory.token(userId, "INVERSIONISTA");
        comprar(token, userId, "ORD-5206", "EXE-5206", "GOOG", "50", "100.00");

        ExecutorService ejecutor = Executors.newFixedThreadPool(10);
        CountDownLatch preparadas = new CountDownLatch(10);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Integer>> futuros = new ArrayList<>();
        try {
            for (int i = 0; i < 10; i++) {
                String sufijo = String.valueOf(i);
                futuros.add(ejecutor.submit(() -> {
                    preparadas.countDown();
                    salida.await(10, TimeUnit.SECONDS);
                    return reservar(token, userId, "ORD-5206-" + sufijo, "GOOG", "10",
                            "clave-5206-" + sufijo, null).getStatusCode().value();
                }));
            }
            assertThat(preparadas.await(10, TimeUnit.SECONDS)).isTrue();
            salida.countDown();

            int creadas = 0;
            int rechazadas = 0;
            for (Future<Integer> futuro : futuros) {
                int estado = futuro.get(60, TimeUnit.SECONDS);
                if (estado == HttpStatus.CREATED.value()) {
                    creadas++;
                } else if (estado == HttpStatus.UNPROCESSABLE_ENTITY.value()) {
                    rechazadas++;
                }
            }
            assertThat(creadas).isEqualTo(5);
            assertThat(rechazadas).isEqualTo(5);
        } finally {
            ejecutor.shutdownNow();
        }

        Map<?, ?> posicion = posicion(token, userId, "GOOG");
        assertThat(numero(posicion, "reservedQuantity")).isEqualTo(50.0);
        assertThat(numero(posicion, "availableQuantity")).isZero();
        Integer pendientes = jdbc.queryForObject("""
                select count(*) from reserva_posicion r
                join posicion p on p.id = r.posicion_id
                where p.user_id = ? and r.estado_reserva = 'ACTIVA'
                """, Integer.class, userId);
        assertThat(pendientes).isEqualTo(5);
    }

    private ResponseEntity<Map> comprar(String token, long userId, String orderId, String executionId,
                                        String symbol, String quantity, String price) {
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("orderId", orderId);
        cuerpo.put("executionId", executionId);
        cuerpo.put("userId", userId);
        cuerpo.put("symbol", symbol);
        cuerpo.put("side", "BUY");
        cuerpo.put("quantity", new BigDecimal(quantity));
        cuerpo.put("price", new BigDecimal(price));
        cuerpo.put("executedAt", EJECUTADO_EN.toString());
        return rest.exchange("/api/v1/positions/updates", HttpMethod.POST,
                new HttpEntity<>(cuerpo, cabeceras(token, null)), Map.class);
    }

    private ResponseEntity<Map> reservar(String token, long userId, String orderId, String symbol,
                                         String quantity, String claveCuerpo, String claveHeader) {
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("orderId", orderId);
        cuerpo.put("userId", userId);
        cuerpo.put("symbol", symbol);
        cuerpo.put("quantity", new BigDecimal(quantity));
        if (claveCuerpo != null) {
            cuerpo.put("idempotencyKey", claveCuerpo);
        }
        return rest.exchange("/api/v1/positions/reservations", HttpMethod.POST,
                new HttpEntity<>(cuerpo, cabeceras(token, claveHeader)), Map.class);
    }

    private ResponseEntity<Map> liberar(String token, long reservaId, String motivo) {
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("reason", motivo);
        return rest.exchange("/api/v1/positions/reservations/" + reservaId + "/release", HttpMethod.POST,
                new HttpEntity<>(cuerpo, cabeceras(token, null)), Map.class);
    }

    private Map<?, ?> posicion(String token, long userId, String symbol) {
        return rest.exchange("/api/v1/positions/" + userId + "/" + symbol, HttpMethod.GET,
                new HttpEntity<>(cabeceras(token, null)), Map.class).getBody();
    }

    private Integer movimientosConClave(String clave) {
        return jdbc.queryForObject("select count(*) from movimiento_posicion where idempotency_key = ?",
                Integer.class, clave);
    }

    private static long idReserva(ResponseEntity<Map> respuesta) {
        return ((Number) respuesta.getBody().get("reservationId")).longValue();
    }

    private static double numero(Map<?, ?> mapa, String clave) {
        return ((Number) mapa.get(clave)).doubleValue();
    }

    private static HttpHeaders cabeceras(String token, String claveIdempotencia) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (claveIdempotencia != null) {
            headers.set(CLAVE_IDEMPOTENCIA, claveIdempotencia);
        }
        return headers;
    }
}

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class M6PosicionesIntegrationTest extends AbstractIntegrationTest {

    private static final Instant PRIMERA = Instant.parse("2026-01-02T10:00:00Z");
    private static final Instant SEGUNDA = PRIMERA.plusSeconds(60);

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void flywayAplicaElEsquemaDePosicionesYMovimientos() {
        Integer migraciones = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where version = '1' and success = true",
                Integer.class);
        Integer tablas = jdbc.queryForObject("""
                select count(*) from information_schema.tables
                where table_schema = 'public' and table_name in ('posicion', 'movimiento_posicion')
                """, Integer.class);

        assertThat(migraciones).isEqualTo(1);
        assertThat(tablas).isEqualTo(2);
    }

    @Test
    void dosComprasCalculanElCostoPonderadoYElPortafolio() {
        long userId = 5101L;
        String token = TokenTestFactory.token(userId, "INVERSIONISTA");

        ResponseEntity<Map> primera = comprar(token, userId, "ORD-5101-A", "EXE-5101-A",
                "AAPL", "BUY", "10", "180.00", PRIMERA);
        ResponseEntity<Map> segunda = comprar(token, userId, "ORD-5101-B", "EXE-5101-B",
                "AAPL", "BUY", "10", "190.00", SEGUNDA);

        assertThat(primera.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(segunda.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> posicion = (Map<?, ?>) segunda.getBody().get("position");
        assertThat(numero(posicion, "quantity")).isEqualTo(20.0);
        assertThat(numero(posicion, "averageCost")).isEqualTo(185.0);
        assertThat(numero(posicion, "currentPrice")).isEqualTo(190.0);
        assertThat(numero(posicion, "marketValue")).isEqualTo(3800.0);
        assertThat(numero(posicion, "unrealizedPnl")).isEqualTo(100.0);

        ResponseEntity<Map> consulta = intercambiar(token, HttpMethod.GET,
                "/api/v1/positions/" + userId + "/AAPL", null);
        assertThat(consulta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(numero(consulta.getBody(), "quantity")).isEqualTo(20.0);
        assertThat(numero(consulta.getBody(), "averageCost")).isEqualTo(185.0);
        assertThat(numero(consulta.getBody(), "currentPrice")).isEqualTo(190.0);

        ResponseEntity<List> lista = rest.exchange("/api/v1/positions/" + userId, HttpMethod.GET,
                new HttpEntity<>(cabeceras(token)), List.class);
        assertThat(lista.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(lista.getBody()).hasSize(1);

        ResponseEntity<Map> portafolio = intercambiar(token, HttpMethod.GET,
                "/api/v1/portfolio/" + userId, null);
        assertThat(portafolio.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(numero(portafolio.getBody(), "totalValue")).isEqualTo(3800.0);
        assertThat(numero(portafolio.getBody(), "totalCost")).isEqualTo(3700.0);
        assertThat(numero(portafolio.getBody(), "unrealizedPnl")).isEqualTo(100.0);
    }

    @Test
    void unaCompraRepetidaNoDuplicaElMovimiento() {
        long userId = 5102L;
        String token = TokenTestFactory.token(userId, "INVERSIONISTA");

        comprar(token, userId, "ORD-5102", "EXE-5102", "MSFT", "BUY", "10", "100.00", PRIMERA);
        ResponseEntity<Map> repetida = comprar(token, userId, "ORD-5102", "EXE-5102",
                "MSFT", "BUY", "10", "100.00", PRIMERA);

        assertThat(repetida.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(repetida.getBody().get("applied")).isEqualTo(false);
        Map<?, ?> posicion = (Map<?, ?>) repetida.getBody().get("position");
        assertThat(numero(posicion, "quantity")).isEqualTo(10.0);
        Integer movimientos = jdbc.queryForObject(
                "select count(*) from movimiento_posicion where execution_id = ?",
                Integer.class, "EXE-5102");
        assertThat(movimientos).isEqualTo(1);
    }

    @Test
    void unUsuarioAjenoNoPuedeAplicarUnaEjecucion() {
        String token = TokenTestFactory.token(5103L, "INVERSIONISTA");

        ResponseEntity<String> respuesta = rest.exchange("/api/v1/positions/updates", HttpMethod.POST,
                new HttpEntity<>(cuerpo(5999L, "ORD-5103", "EXE-5103", "AAPL", "BUY", "1", "100.00", PRIMERA),
                        cabeceras(token)), String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(respuesta.getHeaders().getContentType())
                .isNotNull()
                .matches(tipo -> tipo.isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        assertThat(respuesta.getBody()).contains("\"status\":403");
    }

    @Test
    void unaVentaResponde422HastaImplementarLasReservas() {
        long userId = 5104L;
        String token = TokenTestFactory.token(userId, "INVERSIONISTA");

        ResponseEntity<String> respuesta = rest.exchange("/api/v1/positions/updates", HttpMethod.POST,
                new HttpEntity<>(cuerpo(userId, "ORD-5104", "EXE-5104", "AAPL", "SELL", "1", "100.00", PRIMERA),
                        cabeceras(token)), String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(respuesta.getBody()).contains("VENTA_NO_DISPONIBLE");
    }

    @Test
    void unaCantidadFraccionariaResponde400() {
        long userId = 5105L;
        String token = TokenTestFactory.token(userId, "INVERSIONISTA");

        ResponseEntity<String> respuesta = rest.exchange("/api/v1/positions/updates", HttpMethod.POST,
                new HttpEntity<>(cuerpo(userId, "ORD-5105", "EXE-5105", "AAPL", "BUY", "1.5", "100.00", PRIMERA),
                        cabeceras(token)), String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody()).contains("quantity");
    }

    @Test
    void lasComprasConcurrentesCreanUnaSolaFilaYSumanLaCantidad() throws Exception {
        long userId = 5106L;
        String token = TokenTestFactory.token(userId, "INVERSIONISTA");
        ExecutorService ejecutor = Executors.newFixedThreadPool(2);
        CountDownLatch preparadas = new CountDownLatch(2);
        CountDownLatch salida = new CountDownLatch(1);
        try {
            Future<ResponseEntity<Map>> primera = ejecutor.submit(() ->
                    comprarConcurrente(token, userId, "ORD-5106-A", "EXE-5106-A", "TSLA",
                            preparadas, salida, PRIMERA));
            Future<ResponseEntity<Map>> segunda = ejecutor.submit(() ->
                    comprarConcurrente(token, userId, "ORD-5106-B", "EXE-5106-B", "TSLA",
                            preparadas, salida, PRIMERA.plusSeconds(1)));
            assertThat(preparadas.await(10, TimeUnit.SECONDS)).isTrue();
            salida.countDown();

            assertThat(primera.get(60, TimeUnit.SECONDS).getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(segunda.get(60, TimeUnit.SECONDS).getStatusCode()).isEqualTo(HttpStatus.OK);
        } finally {
            ejecutor.shutdownNow();
        }

        ResponseEntity<Map> posicion = intercambiar(token, HttpMethod.GET,
                "/api/v1/positions/" + userId + "/TSLA", null);
        assertThat(numero(posicion.getBody(), "quantity")).isEqualTo(20.0);
        Integer filas = jdbc.queryForObject(
                "select count(*) from posicion where user_id = ? and symbol = ?",
                Integer.class, userId, "TSLA");
        Integer movimientos = jdbc.queryForObject("""
                select count(*) from movimiento_posicion m
                join posicion p on p.id = m.posicion_id
                where p.user_id = ? and p.symbol = ?
                """, Integer.class, userId, "TSLA");
        assertThat(filas).isEqualTo(1);
        assertThat(movimientos).isEqualTo(2);
    }

    private ResponseEntity<Map> comprarConcurrente(String token, long userId, String orderId,
                                                   String executionId, String symbol,
                                                   CountDownLatch preparadas, CountDownLatch salida,
                                                   Instant ejecutadoEn) throws InterruptedException {
        preparadas.countDown();
        salida.await(10, TimeUnit.SECONDS);
        return comprar(token, userId, orderId, executionId, symbol, "BUY", "10", "200.00", ejecutadoEn);
    }

    private ResponseEntity<Map> comprar(String token, long userId, String orderId, String executionId,
                                        String symbol, String side, String quantity, String price,
                                        Instant ejecutadoEn) {
        return rest.exchange("/api/v1/positions/updates", HttpMethod.POST,
                new HttpEntity<>(cuerpo(userId, orderId, executionId, symbol, side, quantity, price, ejecutadoEn),
                        cabeceras(token)), Map.class);
    }

    private ResponseEntity<Map> intercambiar(String token, HttpMethod metodo, String ruta, Object cuerpo) {
        return rest.exchange(ruta, metodo, new HttpEntity<>(cuerpo, cabeceras(token)), Map.class);
    }

    private static Map<String, Object> cuerpo(long userId, String orderId, String executionId, String symbol,
                                              String side, String quantity, String price, Instant ejecutadoEn) {
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("orderId", orderId);
        cuerpo.put("executionId", executionId);
        cuerpo.put("userId", userId);
        cuerpo.put("symbol", symbol);
        cuerpo.put("side", side);
        cuerpo.put("quantity", new BigDecimal(quantity));
        cuerpo.put("price", new BigDecimal(price));
        cuerpo.put("executedAt", ejecutadoEn.toString());
        return cuerpo;
    }

    private static HttpHeaders cabeceras(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private static double numero(Map<?, ?> mapa, String clave) {
        return ((Number) mapa.get(clave)).doubleValue();
    }
}

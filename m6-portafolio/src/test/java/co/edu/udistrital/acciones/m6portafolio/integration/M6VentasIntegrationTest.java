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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class M6VentasIntegrationTest extends AbstractIntegrationTest {

    private static final Instant EJECUTADO_EN = Instant.parse("2026-02-10T10:00:00Z");

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void ventaCompletaConReservaCalculaElPnlRealizado() {
        long userId = 6601L;
        String token = TokenTestFactory.token(userId, "INVERSIONISTA");
        comprar(token, userId, "ORD-6601", "EXE-6601", "AAPL", "20", "185.00");

        ResponseEntity<Map> reserva = reservar(token, userId, "ORD-6601-R", "AAPL", "10", "clave-6601");
        assertThat(reserva.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        long reservaId = idReserva(reserva);

        ResponseEntity<Map> venta = updates(token, userId, "ORD-6601-R", "EXE-6601-S", "AAPL",
                "SELL", "10", "200.00");
        assertThat(venta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(venta.getBody().get("applied")).isEqualTo(true);

        Map<?, ?> posicion = posicion(token, userId, "AAPL");
        assertThat(numero(posicion, "quantity")).isEqualByComparingTo("10");
        assertThat(numero(posicion, "reservedQuantity")).isEqualByComparingTo("0");
        assertThat(numero(posicion, "availableQuantity")).isEqualByComparingTo("10");
        assertThat(numero(posicion, "realizedPnl")).isEqualByComparingTo(new BigDecimal("150.00"));

        Integer reservaConfirmada = jdbc.queryForObject(
                "select count(*) from reserva_posicion where id = ? and estado_reserva = 'CONSUMIDA'",
                Integer.class, reservaId);
        assertThat(reservaConfirmada).isEqualTo(1);

        assertThat(movimientosConClave("EXE:EXE-6601-S")).isEqualTo(1);
    }

    @Test
    void ventaParcialYLuegoSegundaVentaCompletaLaReserva() {
        long userId = 6102L;
        String token = TokenTestFactory.token(userId, "INVERSIONISTA");
        comprar(token, userId, "ORD-6102", "EXE-6102", "AAPL", "20", "185.00");
        reservar(token, userId, "ORD-6102-R", "AAPL", "10", "clave-6102");

        ResponseEntity<Map> primeraVenta = updates(token, userId, "ORD-6102-R", "EXE-6102-S1",
                "AAPL", "SELL", "4", "200.00");
        assertThat(primeraVenta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(numero(posicion(token, userId, "AAPL"), "quantity")).isEqualByComparingTo("16");
        assertThat(numero(posicion(token, userId, "AAPL"), "reservedQuantity")).isEqualByComparingTo("6");

        Integer reservaPendiente = jdbc.queryForObject(
                "select count(*) from reserva_posicion rp " +
                "join posicion p on p.id = rp.posicion_id " +
                "where p.user_id = ? and rp.estado_reserva = 'ACTIVA'",
                Integer.class, userId);
        assertThat(reservaPendiente).isEqualTo(1);

        ResponseEntity<Map> segundaVenta = updates(token, userId, "ORD-6102-R", "EXE-6102-S2",
                "AAPL", "SELL", "6", "200.00");
        assertThat(segundaVenta.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<?, ?> posicion = posicion(token, userId, "AAPL");
        assertThat(numero(posicion, "quantity")).isEqualByComparingTo("10");
        assertThat(numero(posicion, "reservedQuantity")).isEqualByComparingTo("0");
        assertThat(numero(posicion, "realizedPnl")).isEqualByComparingTo(new BigDecimal("150.00"));

        Integer reservaConfirmada = jdbc.queryForObject(
                "select count(*) from reserva_posicion rp " +
                "join posicion p on p.id = rp.posicion_id " +
                "where p.user_id = ? and rp.estado_reserva = 'CONSUMIDA'",
                Integer.class, userId);
        assertThat(reservaConfirmada).isEqualTo(1);
    }

    @Test
    void ventaDesdeDisponibleDespuesDeLiberar() {
        long userId = 6103L;
        String token = TokenTestFactory.token(userId, "INVERSIONISTA");
        comprar(token, userId, "ORD-6103", "EXE-6103", "AAPL", "20", "185.00");
        ResponseEntity<Map> reserva = reservar(token, userId, "ORD-6103-R", "AAPL", "10", "clave-6103");
        liberar(token, idReserva(reserva), "cancelada");

        ResponseEntity<Map> venta = updates(token, userId, "ORD-6103-X", "EXE-6103-S", "AAPL",
                "SELL", "5", "200.00");
        assertThat(venta.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<?, ?> posicion = posicion(token, userId, "AAPL");
        assertThat(numero(posicion, "quantity")).isEqualByComparingTo("15");
        assertThat(numero(posicion, "reservedQuantity")).isEqualByComparingTo("0");
        assertThat(numero(posicion, "realizedPnl")).isEqualByComparingTo(new BigDecimal("75.00"));
    }

    @Test
    void ventaMayorQueLoPendienteResponde422() {
        long userId = 6104L;
        String token = TokenTestFactory.token(userId, "INVERSIONISTA");
        comprar(token, userId, "ORD-6104", "EXE-6104", "AAPL", "10", "185.00");
        reservar(token, userId, "ORD-6104-R", "AAPL", "5", "clave-6104");

        ResponseEntity<Map> venta = updates(token, userId, "ORD-6104-R", "EXE-6104-S", "AAPL",
                "SELL", "6", "200.00");
        assertThat(venta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        Map<?, ?> posicion = posicion(token, userId, "AAPL");
        assertThat(numero(posicion, "quantity")).isEqualByComparingTo("10");
        assertThat(numero(posicion, "reservedQuantity")).isEqualByComparingTo("5");
    }

    @Test
    void ventaRepetidaNoAplicaDobleMovimiento() {
        long userId = 6105L;
        String token = TokenTestFactory.token(userId, "INVERSIONISTA");
        comprar(token, userId, "ORD-6105", "EXE-6105", "AAPL", "20", "185.00");

        ResponseEntity<Map> primera = updates(token, userId, "ORD-6105", "EXE-6105-S", "AAPL",
                "SELL", "5", "200.00");
        ResponseEntity<Map> repetida = updates(token, userId, "ORD-6105", "EXE-6105-S", "AAPL",
                "SELL", "5", "200.00");

        assertThat(primera.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(primera.getBody().get("applied")).isEqualTo(true);
        assertThat(repetida.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(repetida.getBody().get("applied")).isEqualTo(false);
        assertThat(movimientosConClave("EXE:EXE-6105-S")).isEqualTo(1);
        assertThat(numero(posicion(token, userId, "AAPL"), "quantity")).isEqualByComparingTo("15");
    }

    @Test
    void liberarReservaConfirmadaResponde409() {
        long userId = 6106L;
        String token = TokenTestFactory.token(userId, "INVERSIONISTA");
        comprar(token, userId, "ORD-6106", "EXE-6106", "AAPL", "10", "185.00");
        ResponseEntity<Map> reserva = reservar(token, userId, "ORD-6106-R", "AAPL", "10", "clave-6106");
        long reservaId = idReserva(reserva);

        updates(token, userId, "ORD-6106-R", "EXE-6106-S", "AAPL", "SELL", "10", "200.00");

        ResponseEntity<Map> liberacion = liberar(token, reservaId, "intento posterior");
        assertThat(liberacion.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void portafolioMuestraElPnlRealizado() {
        long userId = 6107L;
        String token = TokenTestFactory.token(userId, "INVERSIONISTA");
        comprar(token, userId, "ORD-6107", "EXE-6107", "AAPL", "20", "185.00");
        reservar(token, userId, "ORD-6107-R", "AAPL", "10", "clave-6107");
        updates(token, userId, "ORD-6107-R", "EXE-6107-S", "AAPL", "SELL", "10", "200.00");

        ResponseEntity<Map> portafolio = rest.exchange("/api/v1/portfolio/" + userId, HttpMethod.GET,
                new HttpEntity<>(cabeceras(token)), Map.class);

        assertThat(portafolio.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(numero(portafolio.getBody(), "realizedPnl")).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    private ResponseEntity<Map> comprar(String token, long userId, String orderId, String executionId,
                                        String symbol, String quantity, String price) {
        return updates(token, userId, orderId, executionId, symbol, "BUY", quantity, price);
    }

    private ResponseEntity<Map> updates(String token, long userId, String orderId, String executionId,
                                        String symbol, String side, String quantity, String price) {
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("orderId", orderId);
        cuerpo.put("executionId", executionId);
        cuerpo.put("userId", userId);
        cuerpo.put("symbol", symbol);
        cuerpo.put("side", side);
        cuerpo.put("quantity", new BigDecimal(quantity));
        cuerpo.put("price", new BigDecimal(price));
        cuerpo.put("executedAt", EJECUTADO_EN.toString());
        return rest.exchange("/api/v1/positions/updates", HttpMethod.POST,
                new HttpEntity<>(cuerpo, cabeceras(token)), Map.class);
    }

    private ResponseEntity<Map> reservar(String token, long userId, String orderId, String symbol,
                                         String quantity, String clave) {
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("orderId", orderId);
        cuerpo.put("userId", userId);
        cuerpo.put("symbol", symbol);
        cuerpo.put("quantity", new BigDecimal(quantity));
        cuerpo.put("idempotencyKey", clave);
        return rest.exchange("/api/v1/positions/reservations", HttpMethod.POST,
                new HttpEntity<>(cuerpo, cabeceras(token)), Map.class);
    }

    private ResponseEntity<Map> liberar(String token, long reservaId, String motivo) {
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("reason", motivo);
        return rest.exchange("/api/v1/positions/reservations/" + reservaId + "/release",
                HttpMethod.POST, new HttpEntity<>(cuerpo, cabeceras(token)), Map.class);
    }

    private Map<?, ?> posicion(String token, long userId, String symbol) {
        return rest.exchange("/api/v1/positions/" + userId + "/" + symbol, HttpMethod.GET,
                new HttpEntity<>(cabeceras(token)), Map.class).getBody();
    }

    private Integer movimientosConClave(String clave) {
        return jdbc.queryForObject("select count(*) from movimiento_posicion where idempotency_key = ?",
                Integer.class, clave);
    }

    private static long idReserva(ResponseEntity<Map> respuesta) {
        return ((Number) respuesta.getBody().get("reservationId")).longValue();
    }

    private static BigDecimal numero(Map<?, ?> mapa, String clave) {
        Object valor = mapa.get(clave);
        if (valor instanceof Number n) {
            return new BigDecimal(n.toString());
        }
        return new BigDecimal(valor.toString());
    }

    private static HttpHeaders cabeceras(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}

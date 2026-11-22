package co.edu.udistrital.acciones.m10suscripcionespagos.integration;

import co.edu.udistrital.acciones.m10suscripcionespagos.domain.EstadoSuscripcion;
import co.edu.udistrital.acciones.m10suscripcionespagos.support.TokenTestFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class M10SuscripcionesIntegrationTest extends AbstractIntegrationTest {

    private static final long USUARIO_ACTIVO = 880001L;
    private static final long USUARIO_HISTORIAL = 880002L;
    private static final long USUARIO_DUPLICADO = 880003L;
    private static final Instant INICIO = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant FIN = Instant.parse("2027-01-01T00:00:00Z");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper json;

    @AfterEach
    void limpiarSuscripciones() {
        jdbc.update("DELETE FROM suscripcion WHERE user_id IN (?, ?, ?)",
                USUARIO_ACTIVO, USUARIO_HISTORIAL, USUARIO_DUPLICADO);
    }

    @Test
    void flywayAplicaLaMigracionDeSuscripciones() {
        Integer aplicadas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '2' AND success = true",
                Integer.class);

        assertThat(aplicadas).isEqualTo(1);
    }

    @Test
    void elCatalogoDevuelveLosDosPreciosEnOrden() throws Exception {
        ResponseEntity<String> respuesta = rest.getForEntity("/api/v1/payments/plans", String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode planes = json.readTree(respuesta.getBody());
        assertThat(planes.isArray()).isTrue();
        assertThat(planes.size()).isEqualTo(1);

        JsonNode plan = planes.get(0);
        assertThat(plan.path("code").asText()).isEqualTo("PREMIUM");
        assertThat(plan.path("name").asText()).isEqualTo("Acciones UD Premium");

        JsonNode precios = plan.path("prices");
        assertThat(precios.size()).isEqualTo(2);
        assertThat(precios.get(0).path("interval").asText()).isEqualTo("MENSUAL");
        assertThat(precios.get(0).path("amount").decimalValue()).isEqualByComparingTo("12.00");
        assertThat(precios.get(0).path("currency").asText()).isEqualTo("USD");
        assertThat(precios.get(1).path("interval").asText()).isEqualTo("ANUAL");
        assertThat(precios.get(1).path("amount").decimalValue()).isEqualByComparingTo("120.00");
    }

    @Test
    void elIndiceUnicoImpideDosSuscripcionesVigentesParaElMismoUsuario() {
        insertar(USUARIO_DUPLICADO, EstadoSuscripcion.ACTIVA, "sub_duplicada_1", INICIO, FIN);

        assertThatThrownBy(() -> insertar(USUARIO_DUPLICADO, EstadoSuscripcion.EN_MORA,
                "sub_duplicada_2", INICIO, FIN))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void elEntitlementConElTokenInternoDevuelveActivoYValidUntil() throws Exception {
        insertar(USUARIO_ACTIVO, EstadoSuscripcion.ACTIVA, "sub_activa", INICIO, FIN);

        JsonNode cuerpo = entitlement(USUARIO_ACTIVO, TokenTestFactory.TOKEN_INTERNO);

        assertThat(cuerpo.path("active").isBoolean()).isTrue();
        assertThat(cuerpo.path("active").asBoolean()).isTrue();
        assertThat(cuerpo.path("validUntil").asText()).isEqualTo("2027-01-01T00:00:00Z");
    }

    @Test
    void elEntitlementConUnTokenIncorrectoResponde401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("f".repeat(64));

        ResponseEntity<String> respuesta = rest.exchange(
                "/internal/subscriptions/{id}/entitlement", HttpMethod.GET,
                new HttpEntity<>(headers), String.class, USUARIO_ACTIVO);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void miSuscripcionDevuelveElEstadoYElPlanDelUsuario() throws Exception {
        insertar(USUARIO_ACTIVO, EstadoSuscripcion.ACTIVA, "sub_mi_activa", INICIO, FIN);

        ResponseEntity<String> respuesta = rest.exchange(
                "/api/v1/subscriptions/me", HttpMethod.GET, new HttpEntity<>(jwt(USUARIO_ACTIVO)),
                String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode cuerpo = json.readTree(respuesta.getBody());
        assertThat(cuerpo.path("userId").asLong()).isEqualTo(USUARIO_ACTIVO);
        assertThat(cuerpo.path("premium").asBoolean()).isTrue();
        assertThat(cuerpo.path("validUntil").asText()).isEqualTo("2027-01-01T00:00:00Z");
        assertThat(cuerpo.path("subscription").path("plan").asText()).isEqualTo("Acciones UD Premium");
        assertThat(cuerpo.path("subscription").path("interval").asText()).isEqualTo("MENSUAL");
        assertThat(cuerpo.path("subscription").path("amount").decimalValue())
                .isEqualByComparingTo("12.00");
        assertThat(cuerpo.path("subscription").path("status").asText()).isEqualTo("ACTIVA");
    }

    @Test
    void miSuscripcionSinHistorialDevuelvePremiumFalsoYSubscriptionNula() throws Exception {
        ResponseEntity<String> respuesta = rest.exchange(
                "/api/v1/subscriptions/me", HttpMethod.GET, new HttpEntity<>(jwt(USUARIO_HISTORIAL)),
                String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode cuerpo = json.readTree(respuesta.getBody());
        assertThat(cuerpo.path("premium").asBoolean()).isFalse();
        assertThat(cuerpo.path("validUntil").isNull()).isTrue();
        assertThat(cuerpo.path("subscription").isNull()).isTrue();
    }

    @Test
    void unaSuscripcionCanceladaNoDaPremiumPeroSeDevuelveEnElHistorial() throws Exception {
        insertar(USUARIO_HISTORIAL, EstadoSuscripcion.CANCELADA, "sub_cancelada", INICIO, FIN);

        ResponseEntity<String> respuesta = rest.exchange(
                "/api/v1/subscriptions/me", HttpMethod.GET, new HttpEntity<>(jwt(USUARIO_HISTORIAL)),
                String.class);

        JsonNode cuerpo = json.readTree(respuesta.getBody());
        assertThat(cuerpo.path("premium").asBoolean()).isFalse();
        assertThat(cuerpo.path("validUntil").isNull()).isTrue();
        assertThat(cuerpo.path("subscription").path("status").asText()).isEqualTo("CANCELADA");
    }

    @Test
    void elEntitlementDeUnaSuscripcionVencidaNoEstaActivo() throws Exception {
        insertar(USUARIO_HISTORIAL, EstadoSuscripcion.VENCIDA, "sub_vencida", INICIO, FIN);

        JsonNode cuerpo = entitlement(USUARIO_HISTORIAL, TokenTestFactory.TOKEN_INTERNO);

        assertThat(cuerpo.path("active").asBoolean()).isFalse();
        assertThat(cuerpo.path("validUntil").isNull()).isTrue();
    }

    private JsonNode entitlement(long userId, String token) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<String> respuesta = rest.exchange(
                "/internal/subscriptions/{id}/entitlement", HttpMethod.GET,
                new HttpEntity<>(headers), String.class, userId);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        return json.readTree(respuesta.getBody());
    }

    private static HttpHeaders jwt(long userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TokenTestFactory.token(userId, "INVERSIONISTA"));
        return headers;
    }

    private void insertar(long userId, EstadoSuscripcion estado, String stripeId,
                          Instant inicio, Instant fin) {
        Long precioId = jdbc.queryForObject(
                "SELECT id FROM precio_plan WHERE stripe_lookup_key = ?",
                Long.class, "acciones_ud_premium_mensual");
        jdbc.update("""
                INSERT INTO suscripcion (user_id, precio_plan_id, stripe_subscription_id, estado,
                    periodo_inicio, periodo_fin, cancelar_al_final, creado_en, actualizado_en, version)
                VALUES (?, ?, ?, ?, ?, ?, FALSE, ?, ?, 0)
                """,
                userId, precioId, stripeId, estado.name(),
                OffsetDateTime.ofInstant(inicio, ZoneOffset.UTC),
                OffsetDateTime.ofInstant(fin, ZoneOffset.UTC),
                OffsetDateTime.ofInstant(inicio, ZoneOffset.UTC),
                OffsetDateTime.ofInstant(inicio, ZoneOffset.UTC));
    }
}

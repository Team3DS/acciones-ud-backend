package co.edu.udistrital.acciones.m8notificaciones.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.udistrital.acciones.m8notificaciones.domain.AvisoException;
import co.edu.udistrital.acciones.m8notificaciones.domain.CondicionPrecio;
import co.edu.udistrital.acciones.m8notificaciones.domain.EventoAviso;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.YearMonth;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Executors;

@org.junit.jupiter.api.extension.ExtendWith(
        org.springframework.boot.test.system.OutputCaptureExtension.class)
class AvisosIT extends IntegrationSupport {
    @Test
    void eventoCreaAvisoYCorreoSoloTrasConfirmacionDeResend() {
        events.procesar(event("m5.orden.ejecutada", Map.of("userId", 1, "orderId", 42)));
        assertThat(count("notificacion")).isEqualTo(1);
        assertThat(count("entrega_correo")).isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT correo_enviado_en IS NULL FROM notificacion",
                                Boolean.class))
                .isTrue();
        assertThat(deliveries.procesar(10)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT estado FROM entrega_correo", String.class))
                .as(
                        "Entrega: %s",
                        jdbc.queryForMap("SELECT estado,error_codigo,intentos FROM entrega_correo"))
                .isEqualTo("ENVIADO");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT correo_enviado_en IS NOT NULL FROM notificacion",
                                Boolean.class))
                .isTrue();
        assertThat(jdbc.queryForObject("SELECT estado FROM entrega_correo", String.class))
                .isEqualTo("ENVIADO");
        HTTP.verify(
                1,
                postRequestedFor(urlEqualTo("/emails"))
                        .withHeader("Authorization", equalTo("Bearer re_fixture_not_a_real_key"))
                        .withHeader("Idempotency-Key", containing("m8/")));
    }

    @Test
    void deduplicaEventoSecuencialYConcurrente() throws Exception {
        EventoAviso event = event("m2.movimiento.creado", Map.of("userId", 1));
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> events.procesar(event));
            var second = executor.submit(() -> events.procesar(event));
            first.get();
            second.get();
        }
        events.procesar(event);
        assertThat(count("inbox_evento")).isEqualTo(1);
        assertThat(count("notificacion")).isEqualTo(1);
        assertThat(count("entrega_correo")).isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT procesado_en IS NOT NULL FROM inbox_evento", Boolean.class))
                .isTrue();
    }

    @Test
    void revierteInboxSiFallaValidacionDelContenido() {
        assertThatThrownBy(() -> events.procesar(event("m5.orden.creada", Map.of("userId", -1))))
                .isInstanceOf(AvisoException.class);
        assertThat(count("inbox_evento")).isZero();
        assertThat(count("notificacion")).isZero();
    }

    @Test
    void otpNuncaApareceEnCentroNiEnTextoPlanoPersistido(
            org.springframework.boot.test.system.CapturedOutput output) throws Exception {
        events.procesar(
                event(
                        "m1.mfa.solicitado",
                        Map.of(
                                "userId",
                                1,
                                "email",
                                "otp@example.test",
                                "code",
                                "987654",
                                "expiresAt",
                                Instant.now().plusSeconds(300).toString())));
        assertThat(count("notificacion")).isZero();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT contenido_cifrado FROM entrega_correo", String.class))
                .doesNotContain("987654", "otp@example.test");
        mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                                        "/api/v1/notifications")
                                .header("Authorization", token(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
        deliveries.procesar(1);
        assertThat(jdbc.queryForObject("SELECT estado FROM entrega_correo", String.class))
                .as(
                        "Entrega OTP: %s",
                        jdbc.queryForMap("SELECT estado,error_codigo,intentos FROM entrega_correo"))
                .isEqualTo("ENVIADO");
        HTTP.verify(
                1, postRequestedFor(urlEqualTo("/emails")).withRequestBody(containing("987654")));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT contenido_cifrado FROM entrega_correo", String.class))
                .isEmpty();
        assertThat(output.getAll()).doesNotContain("987654", "otp@example.test");
    }

    @Test
    void descartaOtpVencidoSinCorreo() {
        events.procesar(
                event(
                        "m1.verificacion.solicitada",
                        Map.of(
                                "userId",
                                1,
                                "email",
                                "otp@example.test",
                                "code",
                                "987654",
                                "expiresAt",
                                Instant.now().minusSeconds(1).toString())));
        assertThat(count("entrega_correo")).isZero();
        assertThat(count("inbox_evento")).isEqualTo(1);
    }

    @Test
    void bienvenidaYRecuperacionSeEntreganFueraDelCentro() {
        events.procesar(
                event(
                        "m1.usuario.registrado",
                        Map.of("userId", 1, "email", "welcome@example.test")));
        events.procesar(
                event(
                        "m1.recuperacion.solicitada",
                        Map.of(
                                "userId",
                                1,
                                "email",
                                "welcome@example.test",
                                "code",
                                "123456",
                                "expiresAt",
                                Instant.now().plusSeconds(300).toString())));
        assertThat(count("notificacion")).isZero();
        assertThat(deliveries.procesar(10)).isEqualTo(2);
    }

    @Test
    void aislaLecturaYMarcadoEntreUsuarios() throws Exception {
        events.procesar(event("m5.orden.creada", Map.of("userId", 1)));
        long id = jdbc.queryForObject("SELECT id FROM notificacion", Long.class);
        mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                                        "/api/v1/notifications")
                                .header("Authorization", token(2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
        mvc.perform(patch("/api/v1/notifications/{id}/read", id).header("Authorization", token(2)))
                .andExpect(status().isNotFound());
        mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                                        "/api/v1/notifications/unread-count")
                                .header("Authorization", token(1)))
                .andExpect(jsonPath("$.count").value(1));
        mvc.perform(patch("/api/v1/notifications/{id}/read", id).header("Authorization", token(1)))
                .andExpect(status().isNoContent());
        mvc.perform(patch("/api/v1/notifications/{id}/read", id).header("Authorization", token(1)))
                .andExpect(status().isNoContent());
        mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                                        "/api/v1/notifications")
                                .param("unreadOnly", "true")
                                .header("Authorization", token(1)))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void aplicaPreferenciasPorUsuarioTipoYCanal() throws Exception {
        preferencia("APP", false);
        preferencia("CORREO", false);
        events.procesar(event("m5.orden.creada", Map.of("userId", 1)));
        assertThat(count("notificacion")).isZero();
        assertThat(count("entrega_correo")).isZero();
        preferencia("APP", true);
        events.procesar(event("m5.orden.creada", Map.of("userId", 1)));
        assertThat(count("notificacion")).isEqualTo(1);
        assertThat(count("entrega_correo")).isZero();
        preferencia("APP", false);
        preferencia("CORREO", true);
        events.procesar(event("m5.orden.creada", Map.of("userId", 1)));
        assertThat(count("notificacion")).isEqualTo(2);
        assertThat(count("entrega_correo")).isEqualTo(1);
        mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                                        "/api/v1/notifications")
                                .header("Authorization", token(1)))
                .andExpect(jsonPath("$.total").value(1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=-1", "page=100001", "size=0", "size=101", "size=abc"})
    void validaPaginacion(String query) throws Exception {
        mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                                        "/api/v1/notifications?" + query)
                                .header("Authorization", token(1)))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"garbage", "Bearer invalid", "Basic dXNlcjpwYXNz"})
    void rechazaCredencialesInvalidas(String header) throws Exception {
        mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                                        "/api/v1/notifications")
                                .header("Authorization", header))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void exigeFirmaCaducidadEIdentidadPositiva() throws Exception {
        var key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String expired =
                Jwts.builder()
                        .subject("1")
                        .expiration(Date.from(Instant.now().minusSeconds(30)))
                        .signWith(key)
                        .compact();
        String noExpiry = Jwts.builder().subject("1").signWith(key).compact();
        String wrongKey =
                Jwts.builder()
                        .subject("1")
                        .expiration(Date.from(Instant.now().plusSeconds(60)))
                        .signWith(
                                Keys.hmacShaKeyFor(
                                        (SECRET + "wrong").getBytes(StandardCharsets.UTF_8)))
                        .compact();
        for (String token : new String[] {expired, noExpiry, wrongKey}) {
            mvc.perform(
                            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                                            "/api/v1/notifications")
                                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                                "/api/v1/notifications"))
                .andExpect(status().isUnauthorized());
        mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                                        "/api/v1/notifications")
                                .header("Authorization", token(-1)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rechazaCamposNoReconocidosYPreferenciasInvalidas() throws Exception {
        mvc.perform(
                        patch("/api/v1/notifications/preferences")
                                .header("Authorization", token(1))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"type\":\"CAMBIO_ESTADO_ORDEN\",\"channel\":\"APP\",\"enabled\":true,\"userId\":2}"))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        patch("/api/v1/notifications/preferences")
                                .header("Authorization", token(1))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"type\":\"MFA\",\"channel\":\"SMS\",\"enabled\":true}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creaAlertasIdempotentesYDetectaCambioDeSolicitud() throws Exception {
        String body = "{\"instrumentId\":1,\"condition\":\"SUPERA\",\"targetPrice\":150}";
        for (int n = 0; n < 2; n++) {
            mvc.perform(
                            org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                    .post("/api/v1/alerts/price")
                                    .header("Authorization", token(1))
                                    .header("Idempotency-Key", "same-key")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.symbol").value("AAPL"));
        }
        assertThat(count("alerta_precio")).isEqualTo(1);
        mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                                        "/api/v1/alerts/price")
                                .header("Authorization", token(1))
                                .header("Idempotency-Key", "same-key")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body.replace("150", "151")))
                .andExpect(status().isConflict());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "1.00001", "100000000000000"})
    void validaUmbrales(String target) throws Exception {
        mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                                        "/api/v1/alerts/price")
                                .header("Authorization", token(1))
                                .header("Idempotency-Key", "key")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"instrumentId\":1,\"condition\":\"SUPERA\",\"targetPrice\":"
                                                + target
                                                + "}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exigePremiumActivoSinConcederloAnteFalloDeM10() {
        entitlement(false);
        assertThatThrownBy(() -> crearAlerta("key"))
                .isInstanceOf(AvisoException.class)
                .hasMessageContaining("premium");
        HTTP.stubFor(
                get(urlPathMatching("/internal/subscriptions/[0-9]+/entitlement"))
                        .willReturn(aResponse().withStatus(503)));
        assertThatThrownBy(() -> crearAlerta("key"))
                .isInstanceOf(AvisoException.class)
                .hasMessageContaining("no disponible");
        assertThat(count("alerta_precio")).isZero();
    }

    @Test
    void suscripcionCaducadaNoHabilitaAlertas() {
        HTTP.stubFor(
                get(urlPathMatching("/internal/subscriptions/[0-9]+/entitlement"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                "{\"active\":true,\"validUntil\":\"2020-01-01T00:00:00Z\"}")));
        assertThatThrownBy(() -> crearAlerta("key")).isInstanceOf(AvisoException.class);
        assertThat(count("alerta_precio")).isZero();
    }

    @Test
    void disparaUnaVezInclusoConEvaluadoresConcurrentes() throws Exception {
        crearAlerta("key");
        quote("150", Instant.now());
        alerts.evaluar();
        assertThat(count("notificacion")).isZero();
        quote("151", Instant.now());
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(alerts::evaluar);
            var second = executor.submit(alerts::evaluar);
            first.get();
            second.get();
        }
        alerts.evaluar();
        assertThat(count("notificacion")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT activa FROM alerta_precio", Boolean.class))
                .isFalse();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT disparada_en IS NOT NULL FROM alerta_precio",
                                Boolean.class))
                .isTrue();
    }

    @Test
    void noDisparaConCotizacionAntiguaFuturaOProveedorCaido() {
        crearAlerta("key");
        quote("151", Instant.now().minusSeconds(180));
        alerts.evaluar();
        quote("151", Instant.now().plusSeconds(60));
        alerts.evaluar();
        HTTP.stubFor(
                get(urlEqualTo("/api/v1/quotes/AAPL")).willReturn(aResponse().withStatus(503)));
        alerts.evaluar();
        assertThat(count("notificacion")).isZero();
        assertThat(jdbc.queryForObject("SELECT activa FROM alerta_precio", Boolean.class)).isTrue();
    }

    @Test
    void desactivaPorFinDePremiumYPorEventoDeCancelacion() {
        crearAlerta("first");
        entitlement(false);
        alerts.evaluar();
        assertThat(jdbc.queryForObject("SELECT activa FROM alerta_precio", Boolean.class))
                .isFalse();
        entitlement(true);
        crearAlerta("second");
        events.procesar(event("m10.suscripcion.cancelada", Map.of("userId", 1)));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM alerta_precio WHERE activa", Integer.class))
                .isZero();
    }

    @Test
    void caidaDePremiumNoDisparaNiDesactivaComoSiFueraCancelacion() {
        crearAlerta("key");
        HTTP.stubFor(
                get(urlPathMatching("/internal/subscriptions/[0-9]+/entitlement"))
                        .willReturn(aResponse().withStatus(503)));
        alerts.evaluar();
        assertThat(count("notificacion")).isZero();
        assertThat(jdbc.queryForObject("SELECT activa FROM alerta_precio", Boolean.class)).isTrue();
    }

    @Test
    void alertasDeOtroUsuarioNoSeListanNiSeEliminan() throws Exception {
        long id = crearAlerta("key").id();
        mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                                        "/api/v1/alerts/price")
                                .header("Authorization", token(2)))
                .andExpect(jsonPath("$").isEmpty());
        mvc.perform(delete("/api/v1/alerts/price/{id}", id).header("Authorization", token(2)))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/alerts/price/{id}", id).header("Authorization", token(1)))
                .andExpect(status().isNoContent());
    }

    @Test
    void distribuyeAvisosSoloASuscritosYSinColisionDeCorreos() throws Exception {
        for (int user = 1; user <= 2; user++) {
            mvc.perform(
                            org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                    .post("/api/v1/notifications/markets/NYSE")
                                    .header("Authorization", token(user)))
                    .andExpect(status().isNoContent());
        }
        EventoAviso event = event("m4.mercado.abierto", Map.of("market", "NYSE"));
        events.procesar(event);
        events.procesar(event);
        assertThat(count("notificacion")).isEqualTo(2);
        assertThat(count("entrega_correo")).isEqualTo(2);
        mvc.perform(delete("/api/v1/notifications/markets/NYSE").header("Authorization", token(2)))
                .andExpect(status().isNoContent());
        events.procesar(event("m4.mercado.cerrado", Map.of("market", "NYSE")));
        assertThat(count("notificacion")).isEqualTo(3);
        mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                                        "/api/v1/notifications/markets")
                                .header("Authorization", token(1)))
                .andExpect(jsonPath("$[0]").value("NYSE"));
        mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                                        "/api/v1/notifications/markets/INVALID")
                                .header("Authorization", token(1)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reporteMensualEsUnicoYConfirmaEnvioSinInventarResultadoNeto() {
        YearMonth month = YearMonth.now(java.time.Clock.systemUTC()).minusMonths(1);
        reports.generar(1, "report@example.test", month);
        reports.generar(1, "report@example.test", month);
        assertThat(count("reporte_mensual")).isEqualTo(1);
        assertThat(count("notificacion")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT mensaje FROM notificacion", String.class))
                .contains("Resultado bruto: 12.5", "Comisiones: 0.8")
                .doesNotContain("neto");
        deliveries.procesar(10);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT enviado_en IS NOT NULL FROM reporte_mensual",
                                Boolean.class))
                .isTrue();
    }

    @Test
    void coordinaReportesPaginadosDeUsuariosQueLosSolicitaron() {
        reports.generarMesAnterior();
        assertThat(count("reporte_mensual")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT usuario_id FROM reporte_mensual", Long.class))
                .isEqualTo(7L);
    }

    @Test
    void noGuardaReportesIncompletosNiDePeriodosAbiertos() {
        assertThatThrownBy(
                        () ->
                                reports.generar(
                                        1,
                                        "a@example.test",
                                        YearMonth.now(java.time.Clock.systemUTC())))
                .isInstanceOf(AvisoException.class);
        HTTP.stubFor(
                get(urlPathMatching("/internal/reports/monthly/[0-9]+"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{\"grossProfit\":0,\"currency\":\"USD\"}")));
        assertThatThrownBy(
                        () ->
                                reports.generar(
                                        1,
                                        "a@example.test",
                                        YearMonth.now(java.time.Clock.systemUTC()).minusMonths(1)))
                .isInstanceOf(AvisoException.class);
        assertThat(count("reporte_mensual")).isZero();
    }

    @Test
    void reintentaResendSinDuplicarAvisoYConMismaClave() {
        events.procesar(event("m5.orden.creada", Map.of("userId", 1)));
        HTTP.stubFor(post(urlEqualTo("/emails")).willReturn(aResponse().withStatus(500)));
        deliveries.procesar(1);
        assertThat(jdbc.queryForObject("SELECT estado FROM entrega_correo", String.class))
                .isEqualTo("PENDIENTE");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT correo_enviado_en IS NULL FROM notificacion",
                                Boolean.class))
                .isTrue();
        jdbc.update("UPDATE entrega_correo SET proximo_intento_en=CURRENT_TIMESTAMP");
        HTTP.stubFor(
                get(urlPathMatching("/internal/users/[0-9]+/notification-contact"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{\"email\":\"changed@example.test\"}")));
        HTTP.stubFor(
                post(urlEqualTo("/emails"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{\"id\":\"confirmed-fixture\"}")));
        deliveries.procesar(1);
        var requests = HTTP.findAll(postRequestedFor(urlEqualTo("/emails")));
        assertThat(requests).hasSize(2);
        assertThat(requests.get(0).getHeader("Idempotency-Key"))
                .isEqualTo(requests.get(1).getHeader("Idempotency-Key"));
        assertThat(requests.get(0).getBodyAsString()).isEqualTo(requests.get(1).getBodyAsString());
        assertThat(count("notificacion")).isEqualTo(1);
    }

    @Test
    void correoRechazadoNoSeMarcaComoEnviado() {
        events.procesar(event("m5.orden.creada", Map.of("userId", 1)));
        HTTP.stubFor(post(urlEqualTo("/emails")).willReturn(aResponse().withStatus(401)));
        deliveries.procesar(10);
        assertThat(jdbc.queryForObject("SELECT estado FROM entrega_correo", String.class))
                .isEqualTo("FALLIDO");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT correo_enviado_en IS NULL FROM notificacion",
                                Boolean.class))
                .isTrue();
    }

    @Test
    void respuestaSinConfirmacionNoEquivaleAEnvio() {
        events.procesar(event("m5.orden.creada", Map.of("userId", 1)));
        HTTP.stubFor(
                post(urlEqualTo("/emails"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{}")));
        deliveries.procesar(1);
        assertThat(jdbc.queryForObject("SELECT estado FROM entrega_correo", String.class))
                .isEqualTo("PENDIENTE");
    }

    @Test
    void noReenviaFueraDeVentanaDeIdempotencia() {
        events.procesar(event("m5.orden.creada", Map.of("userId", 1)));
        jdbc.update("UPDATE entrega_correo SET expira_en=CURRENT_TIMESTAMP-INTERVAL '1 second'");
        deliveries.procesar(10);
        assertThat(jdbc.queryForObject("SELECT estado FROM entrega_correo", String.class))
                .isEqualTo("EXPIRADO");
        HTTP.verify(0, postRequestedFor(urlEqualTo("/emails")));
    }

    @Test
    void limitaReintentosDeCorreo() {
        events.procesar(event("m5.orden.creada", Map.of("userId", 1)));
        HTTP.stubFor(post(urlEqualTo("/emails")).willReturn(aResponse().withStatus(429)));
        for (int n = 0; n < 6; n++) {
            jdbc.update("UPDATE entrega_correo SET proximo_intento_en=CURRENT_TIMESTAMP");
            deliveries.procesar(1);
        }
        assertThat(jdbc.queryForObject("SELECT intentos FROM entrega_correo", Integer.class))
                .isEqualTo(5);
        assertThat(jdbc.queryForObject("SELECT estado FROM entrega_correo", String.class))
                .isEqualTo("FALLIDO");
        HTTP.verify(5, postRequestedFor(urlEqualTo("/emails")));
    }

    @Test
    void trabajadoresConcurrentesNoEnvianLaMismaEntrega() throws Exception {
        events.procesar(event("m5.orden.creada", Map.of("userId", 1)));
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> deliveries.procesar(1));
            var second = executor.submit(() -> deliveries.procesar(1));
            assertThat(first.get() + second.get()).isEqualTo(1);
        }
        HTTP.verify(1, postRequestedFor(urlEqualTo("/emails")));
    }

    @Test
    void exponeOpenApiHealthYCorrelacion() throws Exception {
        mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                                "/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/notifications']").exists());
        mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                                "/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                                        "/api/v1/notifications")
                                .param("size", "0")
                                .header("Authorization", token(1))
                                .header("X-Correlation-Id", "m8-fixture"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.correlationId").value("m8-fixture"));
    }

    @Test
    void adoptaBaseExistenteConBaselineSinBorrarAvisos() throws Exception {
        String schema = "baseline_" + java.util.UUID.randomUUID().toString().replace("-", "");
        javax.sql.DataSource dataSource = java.util.Objects.requireNonNull(jdbc.getDataSource());
        jdbc.execute("CREATE SCHEMA " + schema);
        try {
            try (var connection = dataSource.getConnection()) {
                String previous = connection.getSchema();
                try {
                    connection.setSchema(schema);
                    org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(
                            connection,
                            new org.springframework.core.io.support.EncodedResource(
                                    new org.springframework.core.io.ClassPathResource(
                                            "db/migration/V1__crear_avisos.sql"),
                                    StandardCharsets.UTF_8));
                    try (var statement = connection.createStatement()) {
                        statement.execute(
                                "INSERT INTO notificacion(usuario_id,tipo_evento,titulo,mensaje)"
                                    + " VALUES(9,'EJECUCION_ORDEN','Anterior','Aviso existente')");
                    }
                } finally {
                    connection.setSchema(previous);
                }
            }
            var flyway =
                    org.flywaydb.core.Flyway.configure()
                            .dataSource(dataSource)
                            .schemas(schema)
                            .defaultSchema(schema)
                            .baselineOnMigrate(true)
                            .baselineVersion("1")
                            .locations("classpath:db/migration")
                            .load();
            assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT mensaje FROM " + schema + ".notificacion WHERE id=1",
                                    String.class))
                    .isEqualTo("Aviso existente");
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT visible_app FROM "
                                            + schema
                                            + ".notificacion WHERE id=1",
                                    Boolean.class))
                    .isTrue();
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT count(*) FROM " + schema + ".entrega_correo",
                                    Integer.class))
                    .isZero();
        } finally {
            jdbc.execute("DROP SCHEMA " + schema + " CASCADE");
        }
    }

    private void preferencia(String channel, boolean enabled) throws Exception {
        mvc.perform(
                        patch("/api/v1/notifications/preferences")
                                .header("Authorization", token(1))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"type\":\"CAMBIO_ESTADO_ORDEN\",\"channel\":\""
                                                + channel
                                                + "\",\"enabled\":"
                                                + enabled
                                                + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(20));
    }

    private co.edu.udistrital.acciones.m8notificaciones.domain.AlertaPrecio crearAlerta(
            String key) {
        return alerts.crear(1, 1, CondicionPrecio.SUPERA, new BigDecimal("150"), key);
    }
}

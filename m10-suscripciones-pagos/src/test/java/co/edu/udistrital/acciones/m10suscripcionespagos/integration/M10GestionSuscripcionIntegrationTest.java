package co.edu.udistrital.acciones.m10suscripcionespagos.integration;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.usecase.ProcesarEventosStripeUseCase;
import co.edu.udistrital.acciones.m10suscripcionespagos.support.FirmaStripeTestFactory;
import co.edu.udistrital.acciones.m10suscripcionespagos.support.TokenTestFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
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

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;

class M10GestionSuscripcionIntegrationTest extends AbstractIntegrationTest {

    private static final long USUARIO = 990001L;
    private static final long OTRO = 990002L;
    private static final String SUB = "sub_gestion_990";
    private static final String MENSUAL = "acciones_ud_premium_mensual";
    private static final String ANUAL = "acciones_ud_premium_anual";
    private static final Instant INICIO = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant FIN = Instant.parse("2027-01-01T00:00:00Z");
    private static final Instant FIN_ANUAL = Instant.parse("2028-01-01T00:00:00Z");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private ProcesarEventosStripeUseCase procesarEventos;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private TopicExchange accionesExchange;

    @BeforeEach
    void preparar() {
        STRIPE.resetAll();
        limpiar();
        stubPrecioAnual();
    }

    @AfterEach
    void limpiar() {
        jdbc.update("DELETE FROM recibo WHERE suscripcion_id IN "
                + "(SELECT id FROM suscripcion WHERE user_id IN (?, ?))", USUARIO, OTRO);
        jdbc.update("DELETE FROM suscripcion WHERE user_id IN (?, ?)", USUARIO, OTRO);
        jdbc.update("DELETE FROM sesion_checkout WHERE user_id IN (?, ?)", USUARIO, OTRO);
        jdbc.update("DELETE FROM outbox_evento WHERE payload LIKE ?", "%" + SUB + "%");
        jdbc.update("DELETE FROM evento_stripe WHERE id LIKE 'evt_gestion_%'");
    }

    @Test
    void cancelarProgramaLaCancelacionYConservaElPremium() throws Exception {
        Long id = insertar(USUARIO, MENSUAL, "ACTIVA", false);
        stubActualizacion("active", true, MENSUAL, FIN, null);

        ResponseEntity<String> respuesta = cancelar(id, USUARIO);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode cuerpo = json.readTree(respuesta.getBody());
        assertThat(cuerpo.path("cancelAtPeriodEnd").asBoolean()).isTrue();
        assertThat(cuerpo.path("status").asText()).isEqualTo("ACTIVA");
        assertThat(columnaBool("cancelar_al_final", id)).isTrue();
        STRIPE.verify(postRequestedFor(urlPathEqualTo("/v1/subscriptions/" + SUB))
                .withHeader("Idempotency-Key", equalTo("m10-cancelar-" + id + "-0"))
                .withRequestBody(containing("cancel_at_period_end=true")));
        assertThat(premium(USUARIO)).isTrue();
    }

    @Test
    void cancelarDosVecesNoLlamaAStripeLaSegunda() throws Exception {
        Long id = insertar(USUARIO, MENSUAL, "ACTIVA", true);

        ResponseEntity<String> respuesta = cancelar(id, USUARIO);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json.readTree(respuesta.getBody()).path("cancelAtPeriodEnd").asBoolean()).isTrue();
        STRIPE.verify(0, postRequestedFor(urlPathEqualTo("/v1/subscriptions/" + SUB)));
    }

    @Test
    void cancelarLaSuscripcionDeOtroUsuarioResponde403() {
        Long id = insertar(USUARIO, MENSUAL, "ACTIVA", false);

        assertThat(cancelar(id, OTRO).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        STRIPE.verify(0, postRequestedFor(urlPathEqualTo("/v1/subscriptions/" + SUB)));
    }

    @Test
    void cancelarUnaSuscripcionInexistenteResponde404() {
        assertThat(cancelar(99999999L, USUARIO).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void cancelarUnaSuscripcionCanceladaResponde409() {
        Long id = insertar(USUARIO, MENSUAL, "CANCELADA", false);

        assertThat(cancelar(id, USUARIO).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        STRIPE.verify(0, postRequestedFor(urlPathEqualTo("/v1/subscriptions/" + SUB)));
    }

    @Test
    void cambiarAAnualActualizaElPrecioYElPeriodo() throws Exception {
        Long id = insertar(USUARIO, MENSUAL, "ACTIVA", false);
        stubLectura(MENSUAL);
        stubActualizacion("active", false, ANUAL, FIN_ANUAL, null);

        ResponseEntity<String> respuesta = cambiar(id, "ANUAL", USUARIO);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode cuerpo = json.readTree(respuesta.getBody());
        assertThat(cuerpo.path("interval").asText()).isEqualTo("ANUAL");
        assertThat(cuerpo.path("amount").decimalValue()).isEqualByComparingTo("120.00");
        assertThat(cuerpo.path("currentPeriodEnd").asText()).isEqualTo("2028-01-01T00:00:00Z");
        assertThat(jdbc.queryForObject(
                "SELECT pp.stripe_lookup_key FROM suscripcion s JOIN precio_plan pp "
                        + "ON pp.id = s.precio_plan_id WHERE s.id = ?",
                String.class, id)).isEqualTo(ANUAL);
        STRIPE.verify(postRequestedFor(urlPathEqualTo("/v1/subscriptions/" + SUB))
                .withHeader("Idempotency-Key", equalTo("m10-cambiar-intervalo-" + id + "-0"))
                .withRequestBody(containing("items[0][id]=si_gestion"))
                .withRequestBody(containing("items[0][price]=price_anual_int"))
                .withRequestBody(containing("proration_behavior=always_invoice")));
    }

    @Test
    void cambiarAlMismoIntervaloNoLlamaAStripe() throws Exception {
        Long id = insertar(USUARIO, MENSUAL, "ACTIVA", false);

        ResponseEntity<String> respuesta = cambiar(id, "MENSUAL", USUARIO);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json.readTree(respuesta.getBody()).path("interval").asText()).isEqualTo("MENSUAL");
        STRIPE.verify(0, postRequestedFor(urlPathEqualTo("/v1/subscriptions/" + SUB)));
    }

    @Test
    void cambiarConCancelacionProgramadaResponde409() {
        Long id = insertar(USUARIO, MENSUAL, "ACTIVA", true);

        assertThat(cambiar(id, "ANUAL", USUARIO).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        STRIPE.verify(0, postRequestedFor(urlPathEqualTo("/v1/subscriptions/" + SUB)));
    }

    @Test
    void siStripeFallaAlCambiarNoSeModificaLaSuscripcion() {
        Long id = insertar(USUARIO, MENSUAL, "ACTIVA", false);
        stubLectura(MENSUAL);
        STRIPE.stubFor(post(urlPathEqualTo("/v1/subscriptions/" + SUB))
                .willReturn(aResponse().withStatus(500).withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":{\"type\":\"api_error\",\"message\":\"fallo interno\"}}")));

        assertThat(cambiar(id, "ANUAL", USUARIO).getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(jdbc.queryForObject(
                "SELECT pp.stripe_lookup_key FROM suscripcion s JOIN precio_plan pp "
                        + "ON pp.id = s.precio_plan_id WHERE s.id = ?",
                String.class, id)).isEqualTo(MENSUAL);
    }

    @Test
    void laBajaDeStripeCancelaPublicaElEventoYQuitaElPremium() throws Exception {
        Long id = insertar(USUARIO, MENSUAL, "ACTIVA", true);
        stubLecturaCancelada();
        String cola = "m10.gestion." + UUID.randomUUID();
        Queue destino = new Queue(cola, false, false, true);
        amqpAdmin.declareQueue(destino);
        amqpAdmin.declareBinding(BindingBuilder.bind(destino).to(accionesExchange).with("m10.suscripcion.*"));
        try {
            recibirBaja();

            assertThat(jdbc.queryForObject("SELECT estado FROM suscripcion WHERE id = ?",
                    String.class, id)).isEqualTo("CANCELADA");
            assertThat(premium(USUARIO)).isFalse();
            assertThat(entitlement(USUARIO)).isFalse();

            Message mensaje = rabbitTemplate.receive(cola, 10_000);
            assertThat(mensaje).isNotNull();
            assertThat(mensaje.getMessageProperties().getReceivedRoutingKey())
                    .isEqualTo("m10.suscripcion.cancelada");
            JsonNode sobre = json.readTree(new String(mensaje.getBody(), StandardCharsets.UTF_8));
            assertThat(sobre.path("payload").get("userId").asLong()).isEqualTo(USUARIO);
        } finally {
            amqpAdmin.deleteQueue(cola);
        }
    }

    private void recibirBaja() {
        String cuerpo = FirmaStripeTestFactory.evento("evt_gestion_baja", "customer.subscription.deleted", SUB, false);
        String firma = FirmaStripeTestFactory.firmar("whsec_integracion", cuerpo, Instant.now());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Stripe-Signature", firma);
        ResponseEntity<String> respuesta = rest.exchange("/api/v1/payments/webhook/stripe", HttpMethod.POST,
                new HttpEntity<>(cuerpo.getBytes(StandardCharsets.UTF_8), headers), String.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        procesarEventos.procesarPendientes();
    }

    private ResponseEntity<String> cancelar(long id, long userId) {
        return rest.exchange("/api/v1/subscriptions/" + id + "/cancel", HttpMethod.POST,
                new HttpEntity<>(jwt(userId)), String.class);
    }

    private ResponseEntity<String> cambiar(long id, String intervalo, long userId) {
        HttpHeaders headers = jwt(userId);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange("/api/v1/subscriptions/" + id + "/change-plan", HttpMethod.POST,
                new HttpEntity<>("{\"interval\":\"" + intervalo + "\"}", headers), String.class);
    }

    private static HttpHeaders jwt(long userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TokenTestFactory.token(userId, "INVERSIONISTA"));
        return headers;
    }

    private Long insertar(long userId, String lookup, String estado, boolean cancelarAlFinal) {
        Long precioId = jdbc.queryForObject(
                "SELECT id FROM precio_plan WHERE stripe_lookup_key = ?", Long.class, lookup);
        jdbc.update("""
                INSERT INTO suscripcion (user_id, precio_plan_id, stripe_subscription_id, estado,
                    periodo_inicio, periodo_fin, cancelar_al_final, creado_en, actualizado_en, version)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                """,
                userId, precioId, SUB, estado,
                OffsetDateTime.ofInstant(INICIO, ZoneOffset.UTC),
                OffsetDateTime.ofInstant(FIN, ZoneOffset.UTC),
                cancelarAlFinal,
                OffsetDateTime.ofInstant(INICIO, ZoneOffset.UTC),
                OffsetDateTime.ofInstant(INICIO, ZoneOffset.UTC));
        return jdbc.queryForObject("SELECT id FROM suscripcion WHERE user_id = ? AND stripe_subscription_id = ?",
                Long.class, userId, SUB);
    }

    private boolean columnaBool(String columna, Long id) {
        Boolean valor = jdbc.queryForObject("SELECT " + columna + " FROM suscripcion WHERE id = ?",
                Boolean.class, id);
        return Boolean.TRUE.equals(valor);
    }

    private boolean premium(long userId) throws Exception {
        ResponseEntity<String> respuesta = rest.exchange("/api/v1/subscriptions/me", HttpMethod.GET,
                new HttpEntity<>(jwt(userId)), String.class);
        return json.readTree(respuesta.getBody()).path("premium").asBoolean();
    }

    private boolean entitlement(long userId) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TokenTestFactory.TOKEN_INTERNO);
        ResponseEntity<String> respuesta = rest.exchange(
                "/internal/subscriptions/{id}/entitlement", HttpMethod.GET,
                new HttpEntity<>(headers), String.class, userId);
        return json.readTree(respuesta.getBody()).path("active").asBoolean();
    }

    private void stubPrecioAnual() {
        STRIPE.stubFor(get(urlPathMatching("/v1/prices.*"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"object":"list","data":[{"id":"price_anual_int","object":"price",
                                 "unit_amount":12000,"currency":"usd","lookup_key":"acciones_ud_premium_anual"}]}
                                """)));
    }

    private void stubLectura(String lookup) {
        STRIPE.stubFor(get(urlPathEqualTo("/v1/subscriptions/" + SUB))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(subscriptor("active", false, lookup, FIN, null))));
    }

    private void stubLecturaCancelada() {
        STRIPE.stubFor(get(urlPathEqualTo("/v1/subscriptions/" + SUB))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id":"%s","object":"subscription","status":"canceled",
                                 "cancel_at_period_end":true,"canceled_at":%d,
                                 "ended_at":%d,"cancellation_details":{"reason":"cancellation_requested"},
                                 "customer":"cus_gestion_990","metadata":{"userId":"%d"},
                                 "items":{"object":"list","data":[{"id":"si_gestion",
                                  "object":"subscription_item","current_period_start":%d,
                                  "current_period_end":%d,
                                  "price":{"id":"price_mensual_int","object":"price",
                                  "lookup_key":"%s"}}]}}
                                """.formatted(SUB, FIN.getEpochSecond(), FIN.getEpochSecond(), USUARIO,
                                INICIO.getEpochSecond(), FIN.getEpochSecond(), MENSUAL))));
    }

    private void stubActualizacion(String estado, boolean cancelarAlFinal, String lookup,
                                   Instant fin, Instant endedAt) {
        STRIPE.stubFor(post(urlPathEqualTo("/v1/subscriptions/" + SUB))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(subscriptor(estado, cancelarAlFinal, lookup, fin, endedAt))));
    }

    private String subscriptor(String estado, boolean cancelarAlFinal, String lookup,
                               Instant fin, Instant endedAt) {
        String precioId = ANUAL.equals(lookup) ? "price_anual_int" : "price_mensual_int";
        return """
                {"id":"%s","object":"subscription","status":"%s","cancel_at_period_end":%s,
                 "canceled_at":null,"ended_at":%s,"customer":"cus_gestion_990",
                 "metadata":{"userId":"%d"},
                 "items":{"object":"list","data":[{"id":"si_gestion","object":"subscription_item",
                  "current_period_start":%d,"current_period_end":%d,
                  "price":{"id":"%s","object":"price","lookup_key":"%s"}}]}}
                """.formatted(SUB, estado, cancelarAlFinal,
                endedAt == null ? "null" : String.valueOf(endedAt.getEpochSecond()),
                USUARIO, INICIO.getEpochSecond(), fin.getEpochSecond(), precioId, lookup);
    }
}

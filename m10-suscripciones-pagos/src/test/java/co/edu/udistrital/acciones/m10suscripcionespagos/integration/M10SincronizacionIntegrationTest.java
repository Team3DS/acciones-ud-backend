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
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

class M10SincronizacionIntegrationTest extends AbstractIntegrationTest {

    private static final String SECRETO = "whsec_integracion";
    private static final String RUTA = "/api/v1/payments/webhook/stripe";
    private static final long USUARIO = 990700L;
    private static final String SUB = "sub_sync_700";
    private static final Instant INICIO = Instant.now().minusSeconds(86_400);
    private static final Instant FIN = Instant.now().plusSeconds(30L * 86_400);

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private TopicExchange accionesExchange;

    @Autowired
    private ProcesarEventosStripeUseCase procesarEventos;

    private Long precioMensualId;

    @BeforeEach
    void preparar() {
        STRIPE.resetAll();
        precioMensualId = jdbc.queryForObject(
                "SELECT id FROM precio_plan WHERE stripe_lookup_key = ?",
                Long.class, "acciones_ud_premium_mensual");
        limpiar();
        insertarSesionCompletada();
    }

    @AfterEach
    void limpiar() {
        jdbc.update("DELETE FROM recibo WHERE suscripcion_id IN (SELECT id FROM suscripcion WHERE user_id = ?)",
                USUARIO);
        jdbc.update("DELETE FROM suscripcion WHERE user_id = ?", USUARIO);
        jdbc.update("DELETE FROM sesion_checkout WHERE user_id = ?", USUARIO);
        jdbc.update("DELETE FROM outbox_evento WHERE aggregate_id IS NOT NULL AND payload LIKE ?",
                "%" + SUB + "%");
        jdbc.update("DELETE FROM evento_stripe WHERE id LIKE 'evt_sync_%'");
    }

    @Test
    void elAltaActivaLaSuscripcionYPublicaActivada() throws Exception {
        String cola = "m10.sync." + UUID.randomUUID();
        Queue destino = new Queue(cola, false, false, true);
        amqpAdmin.declareQueue(destino);
        amqpAdmin.declareBinding(BindingBuilder.bind(destino).to(accionesExchange).with("m10.suscripcion.*"));
        try {
            stubSuscripcion("active", null, false);
            recibir("evt_sync_sub_activa", "customer.subscription.updated", SUB);

            procesarEventos.procesarPendientes();

            assertThat(estadoSuscripcion()).isEqualTo("ACTIVA");
            assertThat(premium(USUARIO)).isTrue();
            assertThat(entitlementActivo(USUARIO)).isTrue();

            Message mensaje = rabbitTemplate.receive(cola, 10_000);
            assertThat(mensaje).isNotNull();
            assertThat(mensaje.getMessageProperties().getReceivedRoutingKey())
                    .isEqualTo("m10.suscripcion.activada");
            JsonNode sobre = json.readTree(new String(mensaje.getBody(), StandardCharsets.UTF_8));
            assertThat(sobre.get("source").asText()).isEqualTo("M10");
            assertThat(sobre.get("payload").get("userId").asLong()).isEqualTo(USUARIO);
            assertThat(sobre.get("payload").get("planId").asText()).isEqualTo("PREMIUM");
        } finally {
            amqpAdmin.deleteQueue(cola);
        }
    }

    @Test
    void unCobroFallidoDejaEnMoraConPremiumVigente() throws Exception {
        stubSuscripcion("active", null, false);
        recibir("evt_sync_alta", "customer.subscription.updated", SUB);
        procesarEventos.procesarPendientes();

        stubRecibo("in_sync_fallo", "open", 1);
        stubSuscripcion("past_due", null, false);
        recibir("evt_sync_fallo", "invoice.payment_failed", "in_sync_fallo");
        procesarEventos.procesarPendientes();

        assertThat(estadoSuscripcion()).isEqualTo("EN_MORA");
        assertThat(premium(USUARIO)).isTrue();
        assertThat(contarRecibosFallidos()).isEqualTo(1);
    }

    @Test
    void losRecibosSeListanPaginados() throws Exception {
        stubSuscripcion("active", null, false);
        recibir("evt_sync_alta2", "customer.subscription.updated", SUB);
        procesarEventos.procesarPendientes();

        stubRecibo("in_sync_pagado", "paid", 0);
        recibir("evt_sync_pago", "invoice.paid", "in_sync_pagado");
        procesarEventos.procesarPendientes();

        ResponseEntity<String> respuesta = rest.exchange(
                "/api/v1/payments/invoices?page=0&size=20", HttpMethod.GET,
                new HttpEntity<>(jwt()), String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode cuerpo = json.readTree(respuesta.getBody());
        assertThat(cuerpo.path("content").isArray()).isTrue();
        assertThat(cuerpo.path("content").size()).isEqualTo(1);
        JsonNode recibo = cuerpo.path("content").get(0);
        assertThat(recibo.path("status").asText()).isEqualTo("PAGADO");
        assertThat(recibo.path("amount").decimalValue()).isEqualByComparingTo("12.00");
        assertThat(recibo.path("currency").asText()).isEqualTo("USD");
        assertThat(recibo.path("receiptUrl").asText()).isNotBlank();
    }

    private void insertarSesionCompletada() {
        jdbc.update("""
                INSERT INTO sesion_checkout (user_id, precio_plan_id, stripe_session_id, url, estado,
                    idempotency_key, creado_en, expira_en, completado_en, stripe_customer_id)
                VALUES (?, ?, 'cs_sync_700', 'https://stripe.test/cs_sync_700', 'COMPLETADA',
                    'clave-sync-700', now(), now() + interval '1 hour', now(), 'cus_sync_700')
                """, USUARIO, precioMensualId);
    }

    private void recibir(String id, String tipo, String objetoId) {
        String cuerpo = FirmaStripeTestFactory.evento(id, tipo, objetoId, false);
        String firma = FirmaStripeTestFactory.firmar(SECRETO, cuerpo, Instant.now());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Stripe-Signature", firma);
        ResponseEntity<String> respuesta = rest.exchange(RUTA, HttpMethod.POST,
                new HttpEntity<>(cuerpo.getBytes(StandardCharsets.UTF_8), headers), String.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private void stubSuscripcion(String estado, String motivo, boolean cancelarAlFinal) {
        String cancellation = motivo == null ? "null" : "{\"reason\":\"" + motivo + "\"}";
        STRIPE.stubFor(get(urlPathEqualTo("/v1/subscriptions/" + SUB))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id":"%s","object":"subscription","status":"%s",
                                 "cancel_at_period_end":%s,"canceled_at":null,"ended_at":null,
                                 "cancellation_details":%s,"customer":"cus_sync_700",
                                 "metadata":{"userId":"%d"},
                                 "items":{"object":"list","data":[{"id":"si_1","object":"subscription_item",
                                   "current_period_start":%d,"current_period_end":%d,
                                   "price":{"id":"price_mensual","object":"price",
                                     "lookup_key":"acciones_ud_premium_mensual"}}]}}
                                """.formatted(SUB, estado, cancelarAlFinal, cancellation, USUARIO,
                                INICIO.getEpochSecond(), FIN.getEpochSecond()))));
    }

    private void stubRecibo(String id, String estado, int intentos) {
        STRIPE.stubFor(get(urlPathEqualTo("/v1/invoices/" + id))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id":"%s","object":"invoice","number":"A-700","total":1200,
                                 "currency":"usd","status":"%s","attempt_count":%d,
                                 "period_start":%d,"period_end":%d,"created":%d,
                                 "hosted_invoice_url":"https://stripe.test/i/%s",
                                 "invoice_pdf":"https://stripe.test/i/%s.pdf",
                                 "status_transitions":{"paid_at":%s},
                                 "parent":{"type":"subscription_details",
                                   "subscription_details":{"subscription":"%s"}}}
                                """.formatted(id, estado, intentos, INICIO.getEpochSecond(),
                                FIN.getEpochSecond(), INICIO.getEpochSecond(), id, id,
                                "paid".equals(estado) ? String.valueOf(INICIO.getEpochSecond()) : "null",
                                SUB))));
    }

    private String estadoSuscripcion() {
        return jdbc.queryForObject(
                "SELECT estado FROM suscripcion WHERE user_id = ? AND stripe_subscription_id = ?",
                String.class, USUARIO, SUB);
    }

    private int contarRecibosFallidos() {
        Integer n = jdbc.queryForObject("""
                SELECT count(*) FROM recibo WHERE estado = 'FALLIDO'
                AND suscripcion_id IN (SELECT id FROM suscripcion WHERE user_id = ?)
                """, Integer.class, USUARIO);
        return n == null ? 0 : n;
    }

    private boolean premium(long userId) throws Exception {
        ResponseEntity<String> respuesta = rest.exchange(
                "/api/v1/subscriptions/me", HttpMethod.GET, new HttpEntity<>(jwt(userId)), String.class);
        return json.readTree(respuesta.getBody()).path("premium").asBoolean();
    }

    private boolean entitlementActivo(long userId) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TokenTestFactory.TOKEN_INTERNO);
        ResponseEntity<String> respuesta = rest.exchange(
                "/internal/subscriptions/{id}/entitlement", HttpMethod.GET,
                new HttpEntity<>(headers), String.class, userId);
        return json.readTree(respuesta.getBody()).path("active").asBoolean();
    }

    private HttpHeaders jwt() {
        return jwt(USUARIO);
    }

    private static HttpHeaders jwt(long userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TokenTestFactory.token(userId, "INVERSIONISTA"));
        return headers;
    }
}

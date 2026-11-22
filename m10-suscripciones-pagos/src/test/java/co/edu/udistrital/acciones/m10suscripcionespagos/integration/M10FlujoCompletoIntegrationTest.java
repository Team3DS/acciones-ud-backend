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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;

class M10FlujoCompletoIntegrationTest extends AbstractIntegrationTest {

    private static final long USUARIO = 993001L;
    private static final String SUB = "sub_flujo_993";
    private static final String SESION = "cs_flujo_993";
    private static final String CLIENTE = "cus_flujo_993";
    private static final String FACTURA_FALLO = "in_flujo_fallo";
    private static final String FACTURA_PAGO = "in_flujo_pago";
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

    private Queue cola;
    private final List<String> eventosPublicados = new ArrayList<>();

    @BeforeEach
    void preparar() {
        STRIPE.resetAll();
        SERVICIOS.resetAll();
        limpiar();
        eventosPublicados.clear();
        cola = new Queue("m10.flujo." + UUID.randomUUID(), false, false, false);
        amqpAdmin.declareQueue(cola);
        amqpAdmin.declareBinding(BindingBuilder.bind(cola).to(accionesExchange).with("m10.#"));
        stubPrecios();
        stubSesion();
        stubSuscripcion("active", false, MENSUAL, FIN, null, null);
        stubCatalogo();
        stubMercado();
    }

    @AfterEach
    void limpiar() {
        jdbc.update("DELETE FROM recibo WHERE suscripcion_id IN "
                + "(SELECT id FROM suscripcion WHERE user_id = ?)", USUARIO);
        jdbc.update("DELETE FROM watchlist_item WHERE user_id = ?", USUARIO);
        jdbc.update("DELETE FROM suscripcion WHERE user_id = ?", USUARIO);
        jdbc.update("DELETE FROM sesion_checkout WHERE user_id = ?", USUARIO);
        jdbc.update("DELETE FROM cliente_stripe WHERE user_id = ?", USUARIO);
        jdbc.update("DELETE FROM outbox_evento WHERE payload LIKE ?", "%" + USUARIO + "%");
        jdbc.update("DELETE FROM evento_stripe WHERE id LIKE 'evt_flujo_%'");
        if (cola != null) {
            amqpAdmin.deleteQueue(cola.getName());
        }
    }

    @Test
    void elFlujoCompletoDeAltaPremiumWatchlistCobrosYCancelacionPublicaLosEventos() throws Exception {
        planes();
        checkout();
        altaPremium();
        watchlist();
        cambioAnual();
        cobroFallido();
        pago();
        cancelacionProgramada();
        baja();

        assertThat(eventosPublicados).containsExactly(
                "m10.suscripcion.activada",
                "m10.pago.fallido",
                "m10.suscripcion.activada",
                "m10.suscripcion.cancelada");
    }

    private void planes() throws Exception {
        ResponseEntity<String> respuesta = rest.getForEntity("/api/v1/payments/plans", String.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode planes = json.readTree(respuesta.getBody());
        assertThat(planes.get(0).path("prices")).hasSize(2);
    }

    private void checkout() throws Exception {
        HttpHeaders cabeceras = jwt();
        cabeceras.setContentType(MediaType.APPLICATION_JSON);
        cabeceras.set("Idempotency-Key", "clave-flujo-993");
        ResponseEntity<String> respuesta = rest.exchange("/api/v1/subscriptions", HttpMethod.POST,
                new HttpEntity<>("{\"interval\":\"MENSUAL\"}", cabeceras), String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode sesion = json.readTree(respuesta.getBody());
        assertThat(sesion.path("sessionId").asText()).isEqualTo(SESION);
        assertThat(estadoSesion()).isEqualTo("ABIERTA");
    }

    private void altaPremium() throws Exception {
        recibir("evt_flujo_checkout", "checkout.session.completed", SESION);
        procesarEventos.procesarPendientes();

        assertThat(estadoSuscripcion()).isEqualTo("ACTIVA");
        assertThat(suscripcionId()).isPositive();
        assertThat(premium()).isTrue();
        assertThat(entitlement()).isTrue();
        esperarEvento("m10.suscripcion.activada");
    }

    private void watchlist() throws Exception {
        ResponseEntity<String> alta = peticion(HttpMethod.POST, "/api/v1/watchlist/items",
                "{\"symbol\":\"aapl\"}");
        assertThat(alta.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        JsonNode lista = json.readTree(peticion(HttpMethod.GET, "/api/v1/watchlist", null).getBody());
        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).path("symbol").asText()).isEqualTo("AAPL");
        assertThat(lista.get(0).path("price").decimalValue()).isEqualByComparingTo("200.00");
        assertThat(lista.get(0).path("stale").asBoolean()).isFalse();
        sinEventos();
    }

    private void cambioAnual() throws Exception {
        stubSuscripcion("active", false, MENSUAL, FIN, null, null);
        stubActualizacion("active", false, ANUAL, FIN_ANUAL, null);

        ResponseEntity<String> respuesta = rest.exchange(
                "/api/v1/subscriptions/" + suscripcionId() + "/change-plan", HttpMethod.POST,
                new HttpEntity<>("{\"interval\":\"ANUAL\"}", jsonCabeceras()), String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode cuerpo = json.readTree(respuesta.getBody());
        assertThat(cuerpo.path("interval").asText()).isEqualTo("ANUAL");
        assertThat(cuerpo.path("amount").decimalValue()).isEqualByComparingTo("120.00");
        assertThat(jdbc.queryForObject("""
                SELECT pp.stripe_lookup_key FROM suscripcion s
                JOIN precio_plan pp ON pp.id = s.precio_plan_id WHERE s.user_id = ?
                """, String.class, USUARIO)).isEqualTo(ANUAL);
        sinEventos();
    }

    private void cobroFallido() throws Exception {
        stubFactura(FACTURA_FALLO, "open", 1);
        stubSuscripcion("past_due", false, ANUAL, FIN_ANUAL, null, null);
        recibir("evt_flujo_fallo", "invoice.payment_failed", FACTURA_FALLO);
        procesarEventos.procesarPendientes();

        assertThat(estadoSuscripcion()).isEqualTo("EN_MORA");
        assertThat(premium()).isTrue();
        esperarEvento("m10.pago.fallido");
    }

    private void pago() throws Exception {
        stubFactura(FACTURA_PAGO, "paid", 0);
        stubSuscripcion("active", false, ANUAL, FIN_ANUAL, null, null);
        recibir("evt_flujo_pago", "invoice.paid", FACTURA_PAGO);
        procesarEventos.procesarPendientes();

        assertThat(estadoSuscripcion()).isEqualTo("ACTIVA");
        assertThat(premium()).isTrue();
        assertThat(entitlement()).isTrue();
        esperarEvento("m10.suscripcion.activada");
    }

    private void cancelacionProgramada() throws Exception {
        stubActualizacion("active", true, ANUAL, FIN_ANUAL, null);
        ResponseEntity<String> respuesta = rest.exchange(
                "/api/v1/subscriptions/" + suscripcionId() + "/cancel", HttpMethod.POST,
                new HttpEntity<>(jwt()), String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json.readTree(respuesta.getBody()).path("cancelAtPeriodEnd").asBoolean()).isTrue();
        assertThat(premium()).isTrue();
        sinEventos();
    }

    private void baja() throws Exception {
        stubSuscripcion("canceled", true, ANUAL, FIN_ANUAL, "cancellation_requested", FIN_ANUAL);
        recibir("evt_flujo_baja", "customer.subscription.deleted", SUB);
        procesarEventos.procesarPendientes();

        assertThat(estadoSuscripcion()).isEqualTo("CANCELADA");
        assertThat(premium()).isFalse();
        assertThat(entitlement()).isFalse();
        assertThat(peticion(HttpMethod.GET, "/api/v1/watchlist", null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        esperarEvento("m10.suscripcion.cancelada");
    }

    private ResponseEntity<String> peticion(HttpMethod metodo, String ruta, String cuerpo) {
        HttpHeaders cabeceras = jwt();
        if (cuerpo != null) {
            cabeceras.setContentType(MediaType.APPLICATION_JSON);
        }
        return rest.exchange(ruta, metodo, new HttpEntity<>(cuerpo, cabeceras), String.class);
    }

    private void recibir(String id, String tipo, String objetoId) {
        String cuerpo = FirmaStripeTestFactory.evento(id, tipo, objetoId, false);
        String firma = FirmaStripeTestFactory.firmar("whsec_integracion", cuerpo, Instant.now());
        HttpHeaders cabeceras = new HttpHeaders();
        cabeceras.setContentType(MediaType.APPLICATION_JSON);
        cabeceras.set("Stripe-Signature", firma);
        ResponseEntity<String> respuesta = rest.exchange("/api/v1/payments/webhook/stripe", HttpMethod.POST,
                new HttpEntity<>(cuerpo.getBytes(StandardCharsets.UTF_8), cabeceras), String.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private void esperarEvento(String esperado) {
        Message mensaje = rabbitTemplate.receive(cola.getName(), 10_000);
        assertThat(mensaje).as("Se esperaba el evento " + esperado).isNotNull();
        String routingKey = mensaje.getMessageProperties().getReceivedRoutingKey();
        assertThat(routingKey).isEqualTo(esperado);
        JsonNode sobre;
        try {
            sobre = json.readTree(new String(mensaje.getBody(), StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("El sobre del evento no es JSON", ex);
        }
        assertThat(sobre.path("source").asText()).isEqualTo("M10");
        assertThat(sobre.path("payload").path("userId").asLong()).isEqualTo(USUARIO);
        eventosPublicados.add(routingKey);
    }

    private void sinEventos() {
        Message mensaje = rabbitTemplate.receive(cola.getName(), 1_500);
        assertThat(mensaje).as("No se esperaban eventos nuevos").isNull();
    }

    private HttpHeaders jwt() {
        HttpHeaders cabeceras = new HttpHeaders();
        cabeceras.setBearerAuth(TokenTestFactory.token(USUARIO, "INVERSIONISTA"));
        return cabeceras;
    }

    private HttpHeaders jsonCabeceras() {
        HttpHeaders cabeceras = jwt();
        cabeceras.setContentType(MediaType.APPLICATION_JSON);
        return cabeceras;
    }

    private Long suscripcionId() {
        return jdbc.queryForObject("SELECT id FROM suscripcion WHERE user_id = ?", Long.class, USUARIO);
    }

    private String estadoSuscripcion() {
        return jdbc.queryForObject("SELECT estado FROM suscripcion WHERE user_id = ?", String.class, USUARIO);
    }

    private String estadoSesion() {
        return jdbc.queryForObject("SELECT estado FROM sesion_checkout WHERE user_id = ?", String.class, USUARIO);
    }

    private boolean premium() throws Exception {
        return json.readTree(peticion(HttpMethod.GET, "/api/v1/subscriptions/me", null).getBody())
                .path("premium").asBoolean();
    }

    private boolean entitlement() throws Exception {
        HttpHeaders cabeceras = new HttpHeaders();
        cabeceras.setBearerAuth(TokenTestFactory.TOKEN_INTERNO);
        ResponseEntity<String> respuesta = rest.exchange(
                "/internal/subscriptions/{id}/entitlement", HttpMethod.GET,
                new HttpEntity<>(cabeceras), String.class, USUARIO);
        return json.readTree(respuesta.getBody()).path("active").asBoolean();
    }

    private void stubPrecios() {
        STRIPE.stubFor(get(urlPathMatching("/v1/prices.*"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"object":"list","data":[
                                  {"id":"price_mensual","object":"price","unit_amount":1200,
                                   "currency":"usd","lookup_key":"acciones_ud_premium_mensual"},
                                  {"id":"price_anual_int","object":"price","unit_amount":12000,
                                   "currency":"usd","lookup_key":"acciones_ud_premium_anual"}]}
                                """)));
    }

    private void stubSesion() {
        String cuerpo = """
                {"id":"%s","object":"checkout.session","url":"https://stripe.test/%s",
                 "expires_at":%d,"customer":"%s","subscription":"%s"}
                """.formatted(SESION, SESION, FIN.getEpochSecond(), CLIENTE, SUB);
        STRIPE.stubFor(post(urlPathEqualTo("/v1/checkout/sessions"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(cuerpo)));
        STRIPE.stubFor(get(urlPathEqualTo("/v1/checkout/sessions/" + SESION))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(cuerpo)));
    }

    private void stubSuscripcion(String estado, boolean cancelarAlFinal, String lookup,
                                 Instant fin, String motivo, Instant endedAt) {
        STRIPE.stubFor(get(urlPathEqualTo("/v1/subscriptions/" + SUB))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(suscriptor(estado, cancelarAlFinal, lookup, fin, motivo, endedAt))));
    }

    private void stubActualizacion(String estado, boolean cancelarAlFinal, String lookup,
                                   Instant fin, Instant endedAt) {
        STRIPE.stubFor(post(urlPathEqualTo("/v1/subscriptions/" + SUB))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(suscriptor(estado, cancelarAlFinal, lookup, fin, null, endedAt))));
    }

    private String suscriptor(String estado, boolean cancelarAlFinal, String lookup,
                              Instant fin, String motivo, Instant endedAt) {
        String precioId = ANUAL.equals(lookup) ? "price_anual_int" : "price_mensual";
        String cancelacion = motivo == null ? "null" : "{\"reason\":\"" + motivo + "\"}";
        return """
                {"id":"%s","object":"subscription","status":"%s","cancel_at_period_end":%s,
                 "canceled_at":null,"ended_at":%s,"cancellation_details":%s,
                 "customer":"%s","metadata":{"userId":"%d"},
                 "items":{"object":"list","data":[{"id":"si_flujo","object":"subscription_item",
                   "current_period_start":%d,"current_period_end":%d,
                   "price":{"id":"%s","object":"price","lookup_key":"%s"}}]}}
                """.formatted(SUB, estado, cancelarAlFinal,
                endedAt == null ? "null" : String.valueOf(endedAt.getEpochSecond()),
                cancelacion, CLIENTE, USUARIO, INICIO.getEpochSecond(), fin.getEpochSecond(),
                precioId, lookup);
    }

    private void stubFactura(String id, String estado, int intentos) {
        STRIPE.stubFor(get(urlPathEqualTo("/v1/invoices/" + id))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id":"%s","object":"invoice","number":"A-993","total":12000,
                                 "currency":"usd","status":"%s","attempt_count":%d,
                                 "period_start":%d,"period_end":%d,"created":%d,
                                 "hosted_invoice_url":"https://stripe.test/%s",
                                 "invoice_pdf":"https://stripe.test/%s.pdf",
                                 "status_transitions":{"paid_at":%s},
                                 "parent":{"type":"subscription_details",
                                   "subscription_details":{"subscription":"%s"}}}
                                """.formatted(id, estado, intentos, INICIO.getEpochSecond(),
                                FIN_ANUAL.getEpochSecond(), INICIO.getEpochSecond(), id, id,
                                "paid".equals(estado) ? String.valueOf(INICIO.getEpochSecond()) : "null",
                                SUB))));
    }

    private void stubCatalogo() {
        SERVICIOS.stubFor(get(urlPathMatching("/api/v1/instruments/.*/validation"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"valid\":true}")));
    }

    private void stubMercado() {
        SERVICIOS.stubFor(get(urlPathEqualTo("/api/v1/quotes/latest"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("""
                                [{"symbol":"AAPL","price":200.00,"change":1.00,
                                  "changePct":0.50,"timestamp":"2026-11-22T00:00:00Z"}]
                                """)));
    }
}

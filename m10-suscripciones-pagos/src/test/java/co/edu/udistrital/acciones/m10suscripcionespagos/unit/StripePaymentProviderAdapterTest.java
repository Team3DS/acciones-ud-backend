package co.edu.udistrital.acciones.m10suscripcionespagos.unit;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.ProveedorPagosNoDisponibleException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.RecursoProveedorNoEncontradoException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.proveedor.PrecioProveedor;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.proveedor.SesionProveedor;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.proveedor.SolicitudCheckout;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.adapter.out.stripe.StripePaymentProviderAdapter;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.config.StripeProperties;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.stripe.StripeClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StripePaymentProviderAdapterTest {

    private static final WireMockServer STRIPE = new WireMockServer(options().dynamicPort());

    private StripePaymentProviderAdapter adaptador;

    @BeforeAll
    static void iniciar() {
        STRIPE.start();
    }

    @AfterAll
    static void detener() {
        STRIPE.stop();
    }

    @BeforeEach
    void preparar() {
        STRIPE.resetAll();
        StripeProperties propiedades = new StripeProperties(
                "sk_test_clave", "whsec_secreto", STRIPE.baseUrl(),
                Duration.ofSeconds(1), Duration.ofSeconds(1), 0, null, null, null, null, null);
        StripeClient cliente = StripeClient.builder()
                .setApiKey("sk_test_clave")
                .setApiBase(STRIPE.baseUrl())
                .setConnectTimeout(1000)
                .setReadTimeout(1000)
                .setMaxNetworkRetries(0)
                .build();
        adaptador = new StripePaymentProviderAdapter(cliente, propiedades);
    }

    @Test
    void crearSesionEnviaLosParametrosYElHeaderDeIdempotencia() {
        STRIPE.stubFor(post(urlPathEqualTo("/v1/checkout/sessions"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id":"cs_test_1","object":"checkout.session",
                                 "url":"https://stripe.test/pay/cs_test_1","expires_at":1800000000,
                                 "customer":null,"subscription":null}
                                """)));

        SesionProveedor sesion = adaptador.crearSesionCheckout(new SolicitudCheckout(
                42L, "price_123", Instant.ofEpochSecond(1800000000L),
                "https://app.test/?billing=success", "https://app.test/?billing=cancel",
                null, "m10-checkout-42-clave"));

        assertThat(sesion.id()).isEqualTo("cs_test_1");
        assertThat(sesion.url()).isEqualTo("https://stripe.test/pay/cs_test_1");
        STRIPE.verify(postRequestedFor(urlPathEqualTo("/v1/checkout/sessions"))
                .withHeader("Idempotency-Key", equalTo("m10-checkout-42-clave"))
                .withRequestBody(containing("mode=subscription"))
                .withRequestBody(containing("client_reference_id=42"))
                .withRequestBody(containing("price_123")));
    }

    @Test
    void precioPorLookupKeyEnviaElLookupYMapeaLaRespuesta() {
        STRIPE.stubFor(get(urlPathMatching("/v1/prices.*"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"object":"list","data":[{"id":"price_123","object":"price",
                                 "unit_amount":1200,"currency":"usd","lookup_key":"acciones_ud_premium_mensual"}]}
                                """)));

        PrecioProveedor precio = adaptador.precioPorLookupKey("acciones_ud_premium_mensual");

        assertThat(precio.id()).isEqualTo("price_123");
        assertThat(precio.montoCentavos()).isEqualTo(1200L);
        assertThat(precio.moneda()).isEqualTo("usd");
        STRIPE.verify(getRequestedFor(urlPathEqualTo("/v1/prices"))
                .withQueryParam("lookup_keys[0]", equalTo("acciones_ud_premium_mensual")));
    }

    @Test
    void unLookupSinResultadosEsRecursoNoEncontrado() {
        STRIPE.stubFor(get(urlPathMatching("/v1/prices.*"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"object\":\"list\",\"data\":[]}")));

        assertThatThrownBy(() -> adaptador.precioPorLookupKey("inexistente"))
                .isInstanceOf(RecursoProveedorNoEncontradoException.class);
    }

    @Test
    void un404DeStripeEsRecursoNoEncontrado() {
        STRIPE.stubFor(get(urlPathMatching("/v1/subscriptions/.*"))
                .willReturn(aResponse().withStatus(404).withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"error":{"type":"invalid_request_error","code":"resource_missing",
                                 "message":"No such subscription"}}
                                """)));

        assertThatThrownBy(() -> adaptador.obtenerSuscripcion("sub_x"))
                .isInstanceOf(RecursoProveedorNoEncontradoException.class);
    }

    @Test
    void un500DeStripeEsProveedorNoDisponible() {
        STRIPE.stubFor(post(urlPathEqualTo("/v1/checkout/sessions"))
                .willReturn(aResponse().withStatus(500).withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"error":{"type":"api_error","message":"fallo interno"}}
                                """)));

        assertThatThrownBy(() -> adaptador.crearSesionCheckout(new SolicitudCheckout(
                42L, "price_123", Instant.ofEpochSecond(1800000000L),
                "https://app.test/?billing=success", "https://app.test/?billing=cancel",
                null, "clave-500")))
                .isInstanceOf(ProveedorPagosNoDisponibleException.class);
    }

    @Test
    void unTimeoutEsProveedorNoDisponible() {
        STRIPE.stubFor(post(urlPathEqualTo("/v1/checkout/sessions"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withFixedDelay(2000)
                        .withBody("{\"id\":\"cs_lento\",\"object\":\"checkout.session\"}")));

        assertThatThrownBy(() -> adaptador.crearSesionCheckout(new SolicitudCheckout(
                42L, "price_123", Instant.ofEpochSecond(1800000000L),
                "https://app.test/?billing=success", "https://app.test/?billing=cancel",
                null, "clave-timeout")))
                .isInstanceOf(ProveedorPagosNoDisponibleException.class);
    }

    @Test
    void expirarSesionLlamaAlEndpointDeExpiracion() {
        STRIPE.stubFor(post(urlPathEqualTo("/v1/checkout/sessions/cs_abc/expire"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"cs_abc\",\"object\":\"checkout.session\",\"status\":\"expired\"}")));

        adaptador.expirarSesion("cs_abc");

        STRIPE.verify(postRequestedFor(urlPathEqualTo("/v1/checkout/sessions/cs_abc/expire")));
    }

    @Test
    void siStripeNoEstaConfiguradoNoLlamaAlProveedor() {
        StripeProperties sinClave = new StripeProperties(
                "", "", STRIPE.baseUrl(), Duration.ofSeconds(1), Duration.ofSeconds(1), 0,
                null, null, null, null, null);
        StripeClient cliente = StripeClient.builder().setApiKey("sk_test_no").setApiBase(STRIPE.baseUrl()).build();
        StripePaymentProviderAdapter sinConfig = new StripePaymentProviderAdapter(cliente, sinClave);

        assertThatThrownBy(() -> sinConfig.obtenerSuscripcion("sub_x"))
                .isInstanceOf(ProveedorPagosNoDisponibleException.class);
        STRIPE.verify(0, WireMock.anyRequestedFor(urlPathMatching("/v1/.*")));
    }
}

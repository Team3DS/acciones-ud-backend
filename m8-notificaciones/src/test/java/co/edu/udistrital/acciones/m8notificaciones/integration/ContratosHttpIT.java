package co.edu.udistrital.acciones.m8notificaciones.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.udistrital.acciones.m8notificaciones.application.port.out.CorreoException;
import co.edu.udistrital.acciones.m8notificaciones.application.port.out.CorreoPort;
import co.edu.udistrital.acciones.m8notificaciones.application.port.out.ServiciosPort;
import co.edu.udistrital.acciones.m8notificaciones.domain.AvisoException;
import co.edu.udistrital.acciones.m8notificaciones.domain.Correo;
import co.edu.udistrital.acciones.m8notificaciones.infrastructure.adapter.out.ResendCorreoAdapter;

import com.github.tomakehurst.wiremock.http.Fault;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class ContratosHttpIT extends IntegrationSupport {
    @Autowired ServiciosPort services;
    @Autowired CorreoPort mail;

    @ParameterizedTest
    @ValueSource(
            strings = {
                "{}",
                "{\"active\":\"true\"}",
                "{\"active\":true}",
                "{\"active\":true,\"validUntil\":\"invalido\"}",
                "{\"active\":true,\"validUntil\":null}"
            })
    void respuestasIncompatiblesDePremiumNoConcedenAcceso(String response) {
        stub("/internal/subscriptions/[0-9]+/entitlement", response);
        assertThatThrownBy(() -> services.premium(1))
                .isInstanceOf(AvisoException.class)
                .satisfies(error -> assertThat(((AvisoException) error).status()).isEqualTo(503));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "{\"id\":2,\"symbol\":\"AAPL\",\"enabled\":true}",
                "{\"id\":1,\"symbol\":\"aapl\",\"enabled\":true}",
                "{\"id\":1,\"symbol\":\"AAPL\",\"enabled\":\"true\"}",
                "{\"id\":1,\"enabled\":true}",
                "{\"id\":1,\"symbol\":\"\",\"enabled\":true}"
            })
    void noUsaInstrumentosConIdentidadOFormatoInvalido(String response) {
        stub("/api/v1/instruments/id/1", response);
        assertThatThrownBy(() -> services.instrumento(1)).isInstanceOf(AvisoException.class);
    }

    @Test
    void rechazaTiposFueraDelAlcanceDeAcciones() {
        stub(
                "/api/v1/instruments/id/1",
                "{\"id\":1,\"symbol\":\"SPY\",\"enabled\":true,\"type\":\"ETF\"}");
        assertThatThrownBy(() -> services.instrumento(1))
                .isInstanceOf(AvisoException.class)
                .satisfies(error -> assertThat(((AvisoException) error).status()).isEqualTo(422));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "{\"symbol\":\"MSFT\",\"price\":151,\"timestamp\":\"2026-11-16T12:00:00Z\"}",
                "{\"symbol\":\"AAPL\",\"price\":\"151\"}",
                "{\"symbol\":\"AAPL\",\"price\":0}",
                "{\"symbol\":\"AAPL\",\"price\":-1}",
                "{\"symbol\":\"AAPL\",\"price\":151,\"timestamp\":\"invalido\"}",
                "{\"symbol\":\"AAPL\",\"price\":151}"
            })
    void cotizacionesInvalidasNoSeConviertenEnPreciosUtilizables(String response) {
        stub("/api/v1/quotes/AAPL", response);
        assertThatThrownBy(() -> services.cotizacion("AAPL")).isInstanceOf(AvisoException.class);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "{}",
                "{\"email\":null}",
                "{\"email\":\"\"}",
                "{\"email\":\"invalido\"}",
                "{\"email\":12}"
            })
    void destinatariosInvalidosNoSeAceptanDesdeM1(String response) {
        stub("/internal/users/[0-9]+/notification-contact", response);
        assertThatThrownBy(() -> services.correoUsuario(1)).isInstanceOf(AvisoException.class);
    }

    @Test
    void listaDeSuscriptoresValidaFormaTamanoIdentidadYCorreo() throws Exception {
        for (String response :
                List.of(
                        "{\"content\":{}}",
                        "{\"content\":[{\"userId\":0,\"email\":\"a@example.test\"}]}",
                        "{\"content\":[{\"userId\":1,\"email\":\"invalido\"}]}")) {
            stub("/internal/users/monthly-report-subscribers", response);
            assertThatThrownBy(() -> services.suscriptoresReporte(0))
                    .isInstanceOf(AvisoException.class);
        }
        stub(
                "/internal/users/monthly-report-subscribers",
                mapper.writeValueAsString(
                        Map.of(
                                "content",
                                java.util.Collections.nCopies(
                                        101, Map.of("userId", 1, "email", "a@example.test")))));
        assertThatThrownBy(() -> services.suscriptoresReporte(0))
                .isInstanceOf(AvisoException.class);
    }

    @Test
    void rechazaRespuestaVaciaArrayYDocumentoNoJson() {
        for (String body : List.of("", "[]", "no es JSON")) {
            stub("/internal/subscriptions/[0-9]+/entitlement", body);
            assertThatThrownBy(() -> services.premium(1)).isInstanceOf(AvisoException.class);
        }
    }

    @Test
    void erroresYDesconexionesDeFuentesSePropaganSinInventarDatos() {
        HTTP.stubFor(
                get(urlPathMatching("/internal/subscriptions/[0-9]+/entitlement"))
                        .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));
        assertThatThrownBy(() -> services.premium(1)).isInstanceOf(AvisoException.class);
        stub(
                "/internal/reports/monthly/[0-9]+",
                "{\"grossProfit\":\"12\",\"commissions\":0.2,\"currency\":\"USD\"}");
        assertThatThrownBy(() -> services.resumen(1, YearMonth.of(2026, 10)))
                .isInstanceOf(AvisoException.class);
    }

    @ParameterizedTest
    @CsvSource({
        "400,false",
        "401,false",
        "403,false",
        "409,false",
        "429,true",
        "500,true",
        "502,true"
    })
    void clasificaRechazosDelProveedorSinMarcarEnvios(int status, boolean retryable) {
        HTTP.stubFor(post(urlEqualTo("/emails")).willReturn(aResponse().withStatus(status)));
        assertThatThrownBy(() -> mail.enviar(UUID.randomUUID(), correo("a@example.test")))
                .isInstanceOf(CorreoException.class)
                .satisfies(
                        error ->
                                assertThat(((CorreoException) error).retryable())
                                        .isEqualTo(retryable));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{}", "{\"id\":null}", "{\"id\":\"\"}", "{\"id\":1}", "invalid"})
    void respuestasSinConfirmacionNoAcreditanEntrega(String response) {
        HTTP.stubFor(
                post(urlEqualTo("/emails"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(response)));
        assertThatThrownBy(() -> mail.enviar(UUID.randomUUID(), correo("a@example.test")))
                .isInstanceOf(CorreoException.class)
                .satisfies(error -> assertThat(((CorreoException) error).retryable()).isTrue());
    }

    @Test
    void rechazaIdentificadorDesproporcionadoYFalloDeRed() {
        HTTP.stubFor(
                post(urlEqualTo("/emails"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{\"id\":\"" + "a".repeat(101) + "\"}")));
        assertThatThrownBy(() -> mail.enviar(UUID.randomUUID(), correo("a@example.test")))
                .isInstanceOf(CorreoException.class);
        HTTP.stubFor(
                post(urlEqualTo("/emails"))
                        .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));
        assertThatThrownBy(() -> mail.enviar(UUID.randomUUID(), correo("a@example.test")))
                .isInstanceOf(CorreoException.class)
                .satisfies(error -> assertThat(((CorreoException) error).retryable()).isTrue());
    }

    @Test
    void noInvocaProveedorConConfiguracionODestinatarioInvalidos() {
        for (String recipient : new String[] {null, "invalido", "a".repeat(255)}) {
            assertThatThrownBy(() -> mail.enviar(UUID.randomUUID(), correo(recipient)))
                    .isInstanceOf(CorreoException.class)
                    .satisfies(
                            error -> assertThat(((CorreoException) error).retryable()).isFalse());
        }
        assertThatThrownBy(
                        () ->
                                new ResendCorreoAdapter(HTTP.baseUrl(), "", "a@example.test")
                                        .enviar(UUID.randomUUID(), correo("a@example.test")))
                .isInstanceOf(CorreoException.class);
        assertThatThrownBy(
                        () ->
                                new ResendCorreoAdapter(HTTP.baseUrl(), "fixture", "")
                                        .enviar(UUID.randomUUID(), correo("a@example.test")))
                .isInstanceOf(CorreoException.class);
        HTTP.verify(0, postRequestedFor(urlEqualTo("/emails")));
    }

    @Test
    void contenidoDeCorreoManipuladoFallaCerrado() {
        events.procesar(event("m5.orden.creada", Map.of("userId", 1)));
        jdbc.update("UPDATE entrega_correo SET contenido_cifrado='invalido'");
        deliveries.procesar(10);
        assertThat(jdbc.queryForObject("SELECT estado FROM entrega_correo", String.class))
                .isEqualTo("FALLIDO");
        assertThat(jdbc.queryForObject("SELECT error_codigo FROM entrega_correo", String.class))
                .isEqualTo("CONTENIDO_INVALIDO");
        HTTP.verify(0, postRequestedFor(urlEqualTo("/emails")));
    }

    @Test
    void caidaDeContactoMantieneCorreoPendienteSinEnvio() {
        events.procesar(event("m5.orden.creada", Map.of("userId", 1)));
        HTTP.stubFor(
                get(urlPathMatching("/internal/users/[0-9]+/notification-contact"))
                        .willReturn(aResponse().withStatus(503)));
        deliveries.procesar(1);
        assertThat(jdbc.queryForObject("SELECT estado FROM entrega_correo", String.class))
                .isEqualTo("PENDIENTE");
        assertThat(jdbc.queryForObject("SELECT error_codigo FROM entrega_correo", String.class))
                .isEqualTo("DESTINATARIO_NO_DISPONIBLE");
        HTTP.verify(0, postRequestedFor(urlEqualTo("/emails")));
    }

    private static Correo correo(String to) {
        return new Correo(1, to, "Aviso", "<p>Contenido de prueba</p>");
    }

    private static void stub(String path, String body) {
        HTTP.stubFor(
                get(urlPathMatching(path))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(body)));
    }
}

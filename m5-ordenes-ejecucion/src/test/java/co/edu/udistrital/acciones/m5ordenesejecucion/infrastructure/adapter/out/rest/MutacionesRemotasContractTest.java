package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.rest;

import co.edu.udistrital.acciones.m5ordenesejecucion.domain.LadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.security.AuthenticatedUser;
import co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.security.EmisorTokenInterno;
import co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.security.JwtProperties;
import co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.security.JwtTokenValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MutacionesRemotasContractTest {

    private static final JwtProperties JWT = new JwtProperties("secreto-local-de-pruebas-de-al-menos-32-caracteres");
    private static final Long USUARIO = 42L;

    private MockRestServiceServer server;
    private RestFondosClient fondos;
    private RestPortafolioClient portafolio;

    @BeforeEach
    void preparar() {
        RestTemplate template = new RestTemplate();
        server = MockRestServiceServer.bindTo(template).build();
        EmisorTokenInterno emisor = new EmisorTokenInterno(JWT);
        fondos = new RestFondosClient(template, "http://m2", emisor);
        portafolio = new RestPortafolioClient(template, "http://m6", emisor);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void confirmacionDeFondosAutenticaAlDuenoEIndicaSiFinalizo(boolean finalExecution) {
        var esperado = server.expect(requestTo("http://m2/api/v1/funds/reservations/7/confirm"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(tokenDelDueno())
                .andExpect(jsonPath("$.executionId").value("fill-1"))
                .andExpect(jsonPath("$.amount").value(100.20))
                .andExpect(jsonPath("$.finalExecution").value(finalExecution));
        if (finalExecution) {
            esperado.andExpect(jsonPath("$.expectedExecutionCount").value(2));
        } else {
            esperado.andExpect(jsonPath("$.expectedExecutionCount").doesNotExist());
        }
        esperado.andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        fondos.confirmarReserva(7L, USUARIO, "fill-1", new BigDecimal("100.20"), finalExecution,
                finalExecution ? 2 : null);

        server.verify();
    }

    @Test
    void liberacionDeFondosAutenticaAlDuenoDeLaReserva() {
        server.expect(requestTo("http://m2/api/v1/funds/reservations/7/release"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(tokenDelDueno())
                .andExpect(jsonPath("$.reason").value("Cancelada"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        fondos.liberarReserva(7L, USUARIO, "Cancelada");

        server.verify();
    }

    @Test
    void liberacionDeAccionesAutenticaAlDuenoDeLaReserva() {
        server.expect(requestTo("http://m6/api/v1/positions/reservations/8/release"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(tokenDelDueno())
                .andExpect(jsonPath("$.reason").value("Cancelada"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        portafolio.liberarReserva(8L, USUARIO, "Cancelada");

        server.verify();
    }

    @ParameterizedTest
    @EnumSource(LadoOrden.class)
    void actualizacionDePosicionUsaElContratoBuySellYElDueno(LadoOrden lado) {
        server.expect(requestTo("http://m6/api/v1/positions/updates"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(tokenDelDueno())
                .andExpect(jsonPath("$.side").value(lado == LadoOrden.COMPRA ? "BUY" : "SELL"))
                .andExpect(jsonPath("$.userId").value(USUARIO.intValue()))
                .andExpect(jsonPath("$.executionId").value("fill-1"))
                .andRespond(withSuccess("{\"applied\":true,\"position\":{\"averageCost\":100.25}}", MediaType.APPLICATION_JSON));

        BigDecimal costo = portafolio.actualizarPosicion("1", "fill-1", USUARIO, "AAPL", lado,
                1, new BigDecimal("101.00"), Instant.parse("2026-11-25T15:00:00Z"), "event-1");

        assertEquals(new BigDecimal("100.25"), costo);
        server.verify();
    }

    private static RequestMatcher tokenDelDueno() {
        return request -> {
            String autorizacion = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            assertNotNull(autorizacion);
            assertTrue(autorizacion.startsWith("Bearer "));
            AuthenticatedUser actor = new JwtTokenValidator(JWT).validar(autorizacion.substring(7));
            assertEquals(USUARIO, actor.userId());
            assertEquals(Set.of("INVERSIONISTA"), actor.roles());
        };
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void ventaTotalUsaCosteDeEjecucionAunqueLaPosicionQuedeEnCero(boolean applied) {
        server.expect(requestTo("http://m6/api/v1/positions/updates"))
                .andExpect(tokenDelDueno())
                .andRespond(withSuccess("{\"applied\":" + applied
                        + ",\"averageCost\":100.25,\"position\":{\"quantity\":0,\"averageCost\":0}}",
                        MediaType.APPLICATION_JSON));

        BigDecimal cost = portafolio.actualizarPosicion("1", "sell-1", USUARIO, "AAPL", LadoOrden.VENTA,
                1, new BigDecimal("101.00"), Instant.parse("2026-11-25T15:00:00Z"), "event-1");

        assertEquals(new BigDecimal("100.25"), cost);
        server.verify();
    }

    @Test
    void costeCeroNoSePresentaComoConfirmacionValida() {
        server.expect(requestTo("http://m6/api/v1/positions/updates"))
                .andRespond(withSuccess("{\"averageCost\":0,\"position\":{\"averageCost\":0}}", MediaType.APPLICATION_JSON));

        assertThrows(IllegalStateException.class, () -> portafolio.actualizarPosicion("1", "sell-1", USUARIO,
                "AAPL", LadoOrden.VENTA, 1, new BigDecimal("101.00"), Instant.now(), "event-1"));
        server.verify();
    }

    @Test
    void importesDelMercadoRespetanLosDosDecimalesDeM2() {
        server.expect(requestTo("http://m2/api/v1/funds/reservations"))
                .andExpect(jsonPath("$.amount").value(100.13))
                .andRespond(withSuccess("{\"reservationId\":7}", MediaType.APPLICATION_JSON));
        assertEquals(7L, fondos.reservarFondos("1", USUARIO, new BigDecimal("100.1250"), "reservation-1"));
        server.verify();
        server.reset();
        server.expect(requestTo("http://m2/api/v1/funds/reservations/7/confirm"))
                .andExpect(jsonPath("$.amount").value(100.13))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        fondos.confirmarReserva(7L, USUARIO, "fill-1", new BigDecimal("100.1250"), true, 1);
        server.verify();
    }
}

package co.edu.udistrital.acciones.m6portafolio.unit;

import co.edu.udistrital.acciones.m6portafolio.application.dto.Actor;
import co.edu.udistrital.acciones.m6portafolio.application.dto.EjecucionResponse;
import co.edu.udistrital.acciones.m6portafolio.application.dto.PortafolioResponse;
import co.edu.udistrital.acciones.m6portafolio.application.dto.ReservaPosicionResponse;
import co.edu.udistrital.acciones.m6portafolio.application.usecase.AplicarEjecucionUseCase;
import co.edu.udistrital.acciones.m6portafolio.application.usecase.ConsultarPortafolioUseCase;
import co.edu.udistrital.acciones.m6portafolio.application.usecase.ConsultarPosicionUseCase;
import co.edu.udistrital.acciones.m6portafolio.application.usecase.LiberarReservaUseCase;
import co.edu.udistrital.acciones.m6portafolio.application.usecase.ListarPosicionesUseCase;
import co.edu.udistrital.acciones.m6portafolio.application.usecase.ReservarAccionesUseCase;
import co.edu.udistrital.acciones.m6portafolio.infrastructure.adapter.in.rest.PortafolioController;
import co.edu.udistrital.acciones.m6portafolio.infrastructure.adapter.in.rest.PosicionController;
import co.edu.udistrital.acciones.m6portafolio.infrastructure.config.GlobalExceptionHandler;
import co.edu.udistrital.acciones.m6portafolio.infrastructure.security.ApiProblemWriter;
import co.edu.udistrital.acciones.m6portafolio.infrastructure.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PosicionControllerWebMvcTest {

    private static final long USUARIO = 4101L;
    private static final String RUTA = "/api/v1/positions/updates";
    private static final String RUTA_RESERVAS = "/api/v1/positions/reservations";

    private final AplicarEjecucionUseCase aplicarEjecucion = mock(AplicarEjecucionUseCase.class);
    private final ConsultarPosicionUseCase consultarPosicion = mock(ConsultarPosicionUseCase.class);
    private final ListarPosicionesUseCase listarPosiciones = mock(ListarPosicionesUseCase.class);
    private final ConsultarPortafolioUseCase consultarPortafolio = mock(ConsultarPortafolioUseCase.class);
    private final ReservarAccionesUseCase reservarAcciones = mock(ReservarAccionesUseCase.class);
    private final LiberarReservaUseCase liberarReserva = mock(LiberarReservaUseCase.class);

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(
                    new PosicionController(aplicarEjecucion, consultarPosicion, listarPosiciones,
                            reservarAcciones, liberarReserva),
                    new PortafolioController(consultarPortafolio))
            .setControllerAdvice(new GlobalExceptionHandler(
                    new ApiProblemWriter(Jackson2ObjectMapperBuilder.json().build(), Clock.systemUTC())))
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build();

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void unaCantidadFraccionariaResponde400() throws Exception {
        mockMvc.perform(post(RUTA).with(autenticado()).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("AAPL", "BUY", "1.5", "180.00", "O-1", "E-1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("quantity")));
    }

    @Test
    void unaOrdenFaltanteResponde400() throws Exception {
        mockMvc.perform(post(RUTA).with(autenticado()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"executionId":"E-1","userId":4101,"symbol":"AAPL",
                                 "side":"BUY","quantity":10,"price":180.00}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("orderId")));
    }

    @Test
    void unSimboloInvalidoResponde400() throws Exception {
        mockMvc.perform(post(RUTA).with(autenticado()).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("AAPL US", "BUY", "10", "180.00", "O-1", "E-1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("symbol")));
    }

    @Test
    void unPrecioNoPositivoResponde400() throws Exception {
        mockMvc.perform(post(RUTA).with(autenticado()).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("AAPL", "BUY", "10", "0", "O-1", "E-1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("price")));
    }

    @Test
    void unLadoDesconocidoResponde400() throws Exception {
        mockMvc.perform(post(RUTA).with(autenticado()).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("AAPL", "HOLD", "10", "180.00", "O-1", "E-1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El cuerpo de la peticion no es valido"));
    }

    @Test
    void unaEjecucionValidaResponde200YDelegaEnElCasoDeUso() throws Exception {
        when(aplicarEjecucion.ejecutar(any(), any())).thenReturn(new EjecucionResponse(true, null));

        mockMvc.perform(post(RUTA).with(autenticado()).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("AAPL", "BUY", "10", "180.00", "O-1", "E-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applied").value(true));
    }

    @Test
    void listaPosicionesDelUsuario() throws Exception {
        when(listarPosiciones.ejecutar(eq(USUARIO), any(Actor.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/positions/{userId}", USUARIO).with(autenticado()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void consultaElPortafolioDelUsuario() throws Exception {
        when(consultarPortafolio.ejecutar(eq(USUARIO), any(Actor.class)))
                .thenReturn(new PortafolioResponse(USUARIO, List.of(), BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, "USD", Instant.parse("2026-01-02T12:00:00Z"), false));

        mockMvc.perform(get("/api/v1/portfolio/{userId}", USUARIO).with(autenticado()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USUARIO))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    void unaReservaSinClaveDeIdempotenciaResponde400() throws Exception {
        mockMvc.perform(post(RUTA_RESERVAS).with(autenticado()).contentType(MediaType.APPLICATION_JSON)
                        .content(reserva("ORD-1", "AAPL", "10", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString("idempotencia")));
    }

    @Test
    void clavesDistintasEnElHeaderYEnElCuerpoResponden400() throws Exception {
        mockMvc.perform(post(RUTA_RESERVAS).with(autenticado()).contentType(MediaType.APPLICATION_JSON)
                        .header(PosicionController.CLAVE_IDEMPOTENCIA, "clave-1")
                        .content(reserva("ORD-1", "AAPL", "10", "clave-2")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString("no coinciden")));
    }

    @Test
    void unaReservaConLaClaveDelHeaderResponde201() throws Exception {
        when(reservarAcciones.ejecutar(any(), eq("clave-1"), any()))
                .thenReturn(respuestaReserva("PENDING", null));

        mockMvc.perform(post(RUTA_RESERVAS).with(autenticado()).contentType(MediaType.APPLICATION_JSON)
                        .header(PosicionController.CLAVE_IDEMPOTENCIA, "clave-1")
                        .content(reserva("ORD-1", "AAPL", "10", null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reservationId").value(77))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void unaReservaConLaClaveDelCuerpoResponde201() throws Exception {
        when(reservarAcciones.ejecutar(any(), eq("clave-1"), any()))
                .thenReturn(respuestaReserva("PENDING", null));

        mockMvc.perform(post(RUTA_RESERVAS).with(autenticado()).contentType(MediaType.APPLICATION_JSON)
                        .content(reserva("ORD-1", "AAPL", "10", "clave-1")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value("ORD-1"));
    }

    @Test
    void unaClaveDeMasDe64CaracteresResponde400() throws Exception {
        mockMvc.perform(post(RUTA_RESERVAS).with(autenticado()).contentType(MediaType.APPLICATION_JSON)
                        .header(PosicionController.CLAVE_IDEMPOTENCIA, "x".repeat(65))
                        .content(reserva("ORD-1", "AAPL", "10", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString("64")));
    }

    @Test
    void liberarUnaReservaResponde200() throws Exception {
        when(liberarReserva.ejecutar(eq(77L), eq("cancelada"), any()))
                .thenReturn(respuestaReserva("RELEASED", "cancelada"));

        mockMvc.perform(post(RUTA_RESERVAS + "/{id}/release", 77L).with(autenticado())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"cancelada\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RELEASED"))
                .andExpect(jsonPath("$.reason").value("cancelada"));
    }

    private static ReservaPosicionResponse respuestaReserva(String estado, String motivo) {
        return new ReservaPosicionResponse(77L, "ORD-1", USUARIO, "AAPL", new BigDecimal("10"),
                BigDecimal.ZERO, estado, motivo, Instant.parse("2026-02-03T10:00:00Z"), null, null);
    }

    private static String reserva(String orderId, String symbol, String quantity, String clave) {
        String idempotencia = clave == null ? "" : ",\"idempotencyKey\":\"" + clave + "\"";
        return """
                {"orderId":"%s","userId":4101,"symbol":"%s","quantity":%s%s}
                """.formatted(orderId, symbol, quantity, idempotencia);
    }

    private static String cuerpo(String symbol, String side, String quantity, String price,
                                 String orderId, String executionId) {
        return """
                {"orderId":"%s","executionId":"%s","userId":4101,"symbol":"%s",
                 "side":"%s","quantity":%s,"price":%s}
                """.formatted(orderId, executionId, symbol, side, quantity, price);
    }

    private static RequestPostProcessor autenticado() {
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(autenticacion());
            return request;
        };
    }

    private static Authentication autenticacion() {
        AuthenticatedUser usuario = new AuthenticatedUser(USUARIO, Set.of("INVERSIONISTA"));
        return UsernamePasswordAuthenticationToken.authenticated(usuario, null, List.of());
    }
}

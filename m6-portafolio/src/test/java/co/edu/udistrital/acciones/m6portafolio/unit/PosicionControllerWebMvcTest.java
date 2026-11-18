package co.edu.udistrital.acciones.m6portafolio.unit;

import co.edu.udistrital.acciones.m6portafolio.application.dto.Actor;
import co.edu.udistrital.acciones.m6portafolio.application.dto.EjecucionResponse;
import co.edu.udistrital.acciones.m6portafolio.application.dto.PortafolioResponse;
import co.edu.udistrital.acciones.m6portafolio.application.usecase.AplicarEjecucionUseCase;
import co.edu.udistrital.acciones.m6portafolio.application.usecase.ConsultarPortafolioUseCase;
import co.edu.udistrital.acciones.m6portafolio.application.usecase.ConsultarPosicionUseCase;
import co.edu.udistrital.acciones.m6portafolio.application.usecase.ListarPosicionesUseCase;
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

    private final AplicarEjecucionUseCase aplicarEjecucion = mock(AplicarEjecucionUseCase.class);
    private final ConsultarPosicionUseCase consultarPosicion = mock(ConsultarPosicionUseCase.class);
    private final ListarPosicionesUseCase listarPosiciones = mock(ListarPosicionesUseCase.class);
    private final ConsultarPortafolioUseCase consultarPortafolio = mock(ConsultarPortafolioUseCase.class);

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(
                    new PosicionController(aplicarEjecucion, consultarPosicion, listarPosiciones),
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

package co.edu.udistrital.acciones.m2cuentasfondos.unit;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.Actor;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.ReservaRequest;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.ReservaResponse;
import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.AccesoDenegadoException;
import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.ConflictoReservaException;
import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.RecursoNoEncontradoException;
import co.edu.udistrital.acciones.m2cuentasfondos.application.usecase.ConsultarReservaUseCase;
import co.edu.udistrital.acciones.m2cuentasfondos.application.usecase.ReservarFondosUseCase;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.excepcion.FondosInsuficientesException;
import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.adapter.in.rest.ReservasController;
import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.config.SecurityConfig;
import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.config.WebConfig;
import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.security.AuthenticatedUser;
import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.security.JwtTokenValidator;
import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.security.ProblemDetailsWriter;
import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.security.RestAccessDeniedHandler;
import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.security.RestAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReservasController.class)
@Import({SecurityConfig.class, WebConfig.class, RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class, ProblemDetailsWriter.class})
class ReservasWebMvcTest {

    private static final Instant AHORA = Instant.parse("2026-11-11T10:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReservarFondosUseCase reservarFondos;

    @MockBean
    private ConsultarReservaUseCase consultarReserva;

    @MockBean
    private JwtTokenValidator jwtTokenValidator;

    @Test
    void elDuenoReservaConLaClaveEnLaCabecera() throws Exception {
        when(reservarFondos.ejecutar(any(ReservaRequest.class), eq("clave-1"), any(Actor.class)))
                .thenReturn(respuesta());

        mockMvc.perform(post("/api/v1/funds/reservations")
                        .header("Idempotency-Key", "clave-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("orden-1", "300.00"))
                        .with(autenticado(5L, "INVERSIONISTA")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reservationId").value(77))
                .andExpect(jsonPath("$.orderId").value("orden-1"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.available").value(700.00))
                .andExpect(jsonPath("$.reserved").value(300.00))
                .andExpect(jsonPath("$.total").value(1000.00))
                .andExpect(jsonPath("$.currency").value("USD"));

        ArgumentCaptor<ReservaRequest> captor = ArgumentCaptor.forClass(ReservaRequest.class);
        verify(reservarFondos).ejecutar(captor.capture(), eq("clave-1"), any(Actor.class));
        assertThat(captor.getValue().orderId()).isEqualTo("orden-1");
        assertThat(captor.getValue().amount()).isEqualByComparingTo("300.00");
    }

    @Test
    void aceptaLaClaveEnElCuerpo() throws Exception {
        when(reservarFondos.ejecutar(any(ReservaRequest.class), eq("clave-cuerpo"), any(Actor.class)))
                .thenReturn(respuesta());

        mockMvc.perform(post("/api/v1/funds/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":\"orden-1\",\"userId\":5,\"amount\":300.00,"
                                + "\"idempotencyKey\":\"clave-cuerpo\"}")
                        .with(autenticado(5L, "INVERSIONISTA")))
                .andExpect(status().isCreated());

        verify(reservarFondos).ejecutar(any(ReservaRequest.class), eq("clave-cuerpo"), any(Actor.class));
    }

    @Test
    void aceptaLaClaveEnCuerpoYCabeceraSiCoinciden() throws Exception {
        when(reservarFondos.ejecutar(any(ReservaRequest.class), eq("clave-1"), any(Actor.class)))
                .thenReturn(respuesta());

        mockMvc.perform(post("/api/v1/funds/reservations")
                        .header("Idempotency-Key", "clave-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":\"orden-1\",\"userId\":5,\"amount\":300.00,"
                                + "\"idempotencyKey\":\"clave-1\"}")
                        .with(autenticado(5L, "INVERSIONISTA")))
                .andExpect(status().isCreated());
    }

    @Test
    void clavesDistintasEnCuerpoYCabeceraResponden400() throws Exception {
        mockMvc.perform(post("/api/v1/funds/reservations")
                        .header("Idempotency-Key", "clave-cabecera")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":\"orden-1\",\"userId\":5,\"amount\":300.00,"
                                + "\"idempotencyKey\":\"clave-cuerpo\"}")
                        .with(autenticado(5L, "INVERSIONISTA")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void sinClaveDeIdempotenciaResponde400() throws Exception {
        mockMvc.perform(post("/api/v1/funds/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("orden-1", "300.00"))
                        .with(autenticado(5L, "INVERSIONISTA")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void unMontoInvalidoResponde400() throws Exception {
        mockMvc.perform(post("/api/v1/funds/reservations")
                        .header("Idempotency-Key", "clave-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("orden-1", "0"))
                        .with(autenticado(5L, "INVERSIONISTA")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void sinFondosResponde422() throws Exception {
        when(reservarFondos.ejecutar(any(ReservaRequest.class), any(), any(Actor.class)))
                .thenThrow(new FondosInsuficientesException("Fondos insuficientes"));

        mockMvc.perform(post("/api/v1/funds/reservations")
                        .header("Idempotency-Key", "clave-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("orden-1", "300.00"))
                        .with(autenticado(5L, "INVERSIONISTA")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void unaOrdenDuplicadaResponde409() throws Exception {
        when(reservarFondos.ejecutar(any(ReservaRequest.class), any(), any(Actor.class)))
                .thenThrow(new ConflictoReservaException("Ya existe una reserva para la orden orden-1"));

        mockMvc.perform(post("/api/v1/funds/reservations")
                        .header("Idempotency-Key", "clave-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("orden-1", "300.00"))
                        .with(autenticado(5L, "INVERSIONISTA")))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void elDuenoConsultaSuReserva() throws Exception {
        when(consultarReserva.ejecutar(eq(77L), any(Actor.class))).thenReturn(respuesta());

        mockMvc.perform(get("/api/v1/funds/reservations/77")
                        .with(autenticado(5L, "INVERSIONISTA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationId").value(77))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.reserved").value(300.00));
    }

    @Test
    void unaReservaAjenaResponde403() throws Exception {
        when(consultarReserva.ejecutar(eq(77L), any(Actor.class)))
                .thenThrow(new AccesoDenegadoException("No tiene permiso para consultar esta reserva"));

        mockMvc.perform(get("/api/v1/funds/reservations/77")
                        .with(autenticado(9L, "INVERSIONISTA")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void unaReservaInexistenteResponde404() throws Exception {
        when(consultarReserva.ejecutar(eq(77L), any(Actor.class)))
                .thenThrow(new RecursoNoEncontradoException("No existe la reserva 77"));

        mockMvc.perform(get("/api/v1/funds/reservations/77")
                        .with(autenticado(5L, "INVERSIONISTA")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void sinTokenResponde401() throws Exception {
        mockMvc.perform(get("/api/v1/funds/reservations/77"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401));
    }

    private ReservaResponse respuesta() {
        return new ReservaResponse(77L, "orden-1", 5L, new BigDecimal("300.00"), "PENDING",
                new BigDecimal("700.00"), new BigDecimal("300.00"), new BigDecimal("1000.00"), "USD", AHORA);
    }

    private String cuerpo(String orden, String monto) {
        return "{\"orderId\":\"" + orden + "\",\"userId\":5,\"amount\":" + monto + "}";
    }

    private RequestPostProcessor autenticado(Long userId, String... roles) {
        List<GrantedAuthority> autoridades = Arrays.stream(roles)
                .map(rol -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + rol))
                .toList();
        AuthenticatedUser usuario = new AuthenticatedUser(userId, Set.of(roles));
        return authentication(new UsernamePasswordAuthenticationToken(usuario, null, autoridades));
    }
}

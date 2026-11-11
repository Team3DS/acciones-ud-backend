package co.edu.udistrital.acciones.m2cuentasfondos.unit;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.Actor;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.DepositoResponse;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.SaldoResponse;
import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.AccesoDenegadoException;
import co.edu.udistrital.acciones.m2cuentasfondos.application.usecase.ConsultarSaldoUseCase;
import co.edu.udistrital.acciones.m2cuentasfondos.application.usecase.RegistrarDepositoUseCase;
import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.adapter.in.rest.CuentasController;
import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.adapter.in.rest.FondosController;
import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.config.SecurityConfig;
import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.security.AuthenticatedUser;
import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.security.JwtTokenValidator;
import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.security.ProblemDetailsWriter;
import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.security.RestAccessDeniedHandler;
import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.security.RestAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {FondosController.class, CuentasController.class})
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        ProblemDetailsWriter.class})
class CuentasWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RegistrarDepositoUseCase registrarDeposito;

    @MockBean
    private ConsultarSaldoUseCase consultarSaldo;

    @MockBean
    private JwtTokenValidator jwtTokenValidator;

    @Test
    void unAdministradorRegistraUnDeposito() throws Exception {
        when(registrarDeposito.ejecutar(any(), eq("clave-1"), any(Actor.class)))
                .thenReturn(new DepositoResponse(99L, 5L, new BigDecimal("1000.00"),
                        new BigDecimal("1000.00"), new BigDecimal("0.00"), new BigDecimal("1000.00"),
                        "USD", Instant.parse("2026-11-11T10:00:00Z")));

        mockMvc.perform(post("/api/v1/funds/deposits")
                        .header("Idempotency-Key", "clave-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":5,\"amount\":1000.00,\"description\":\"Abono\"}")
                        .with(autenticado(1L, "ADMINISTRADOR")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.movementId").value(99))
                .andExpect(jsonPath("$.userId").value(5))
                .andExpect(jsonPath("$.amount").value(1000.00))
                .andExpect(jsonPath("$.available").value(1000.00))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    void sinCabeceraDeIdempotenciaResponde400() throws Exception {
        mockMvc.perform(post("/api/v1/funds/deposits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":5,\"amount\":10.00}")
                        .with(autenticado(1L, "ADMINISTRADOR")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void unMontoConTresDecimalesResponde400() throws Exception {
        mockMvc.perform(post("/api/v1/funds/deposits")
                        .header("Idempotency-Key", "clave-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":5,\"amount\":10.123}")
                        .with(autenticado(1L, "ADMINISTRADOR")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void sinTokenResponde401() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/5/balance"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void unInversionistaNoPuedeDepositar() throws Exception {
        mockMvc.perform(post("/api/v1/funds/deposits")
                        .header("Idempotency-Key", "clave-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":5,\"amount\":1000.00}")
                        .with(autenticado(5L, "INVERSIONISTA")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void elDuenoConsultaSuSaldo() throws Exception {
        when(consultarSaldo.ejecutar(eq(5L), any(Actor.class)))
                .thenReturn(new SaldoResponse(5L, new BigDecimal("1000.00"),
                        new BigDecimal("0.00"), new BigDecimal("1000.00"), "USD"));

        mockMvc.perform(get("/api/v1/accounts/5/balance")
                        .with(autenticado(5L, "INVERSIONISTA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(5))
                .andExpect(jsonPath("$.available").value(1000.00))
                .andExpect(jsonPath("$.reserved").value(0.00))
                .andExpect(jsonPath("$.total").value(1000.00))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    void unUsuarioAjenoNoPuedeConsultarElSaldo() throws Exception {
        when(consultarSaldo.ejecutar(eq(9L), any(Actor.class)))
                .thenThrow(new AccesoDenegadoException("No tiene permiso para consultar esta cuenta"));

        mockMvc.perform(get("/api/v1/accounts/9/balance")
                        .with(autenticado(5L, "INVERSIONISTA")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403));
    }

    private RequestPostProcessor autenticado(Long userId, String... roles) {
        List<GrantedAuthority> autoridades = Arrays.stream(roles)
                .map(rol -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + rol))
                .toList();
        AuthenticatedUser usuario = new AuthenticatedUser(userId, Set.of(roles));
        return authentication(new UsernamePasswordAuthenticationToken(usuario, null, autoridades));
    }
}

package co.edu.udistrital.acciones.m2cuentasfondos.unit;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.Actor;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.DepositoResponse;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.FiltroMovimientos;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.MovimientoResponse;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.RetiroResponse;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.SaldoResponse;
import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.AccesoDenegadoException;
import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.OrdenInvalidoException;
import co.edu.udistrital.acciones.m2cuentasfondos.application.usecase.ConsultarSaldoUseCase;
import co.edu.udistrital.acciones.m2cuentasfondos.application.usecase.ListarMovimientosUseCase;
import co.edu.udistrital.acciones.m2cuentasfondos.application.usecase.RegistrarDepositoUseCase;
import co.edu.udistrital.acciones.m2cuentasfondos.application.usecase.SolicitarRetiroUseCase;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.TipoMovimiento;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.excepcion.FondosInsuficientesException;
import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.adapter.in.rest.CuentasController;
import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.adapter.in.rest.FondosController;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

@WebMvcTest(controllers = {FondosController.class, CuentasController.class})
@Import({SecurityConfig.class, WebConfig.class, RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class, ProblemDetailsWriter.class})
class CuentasWebMvcTest {

    private static final Instant AHORA = Instant.parse("2026-11-11T10:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RegistrarDepositoUseCase registrarDeposito;

    @MockBean
    private SolicitarRetiroUseCase solicitarRetiro;

    @MockBean
    private ConsultarSaldoUseCase consultarSaldo;

    @MockBean
    private ListarMovimientosUseCase listarMovimientos;

    @MockBean
    private JwtTokenValidator jwtTokenValidator;

    @Test
    void unAdministradorRegistraUnDeposito() throws Exception {
        when(registrarDeposito.ejecutar(any(), eq("clave-1"), any(Actor.class)))
                .thenReturn(new DepositoResponse(99L, 5L, new BigDecimal("1000.00"),
                        new BigDecimal("1000.00"), new BigDecimal("0.00"), new BigDecimal("1000.00"),
                        "USD", AHORA));

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
    void elDuenoRetiraDeSuCuenta() throws Exception {
        when(solicitarRetiro.ejecutar(any(), eq("retiro-1"), any(Actor.class)))
                .thenReturn(new RetiroResponse(120L, 5L, new BigDecimal("200.00"),
                        new BigDecimal("800.00"), new BigDecimal("0.00"), new BigDecimal("800.00"),
                        "USD", AHORA));

        mockMvc.perform(post("/api/v1/funds/withdrawals")
                        .header("Idempotency-Key", "retiro-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":5,\"amount\":200.00}")
                        .with(autenticado(5L, "INVERSIONISTA")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.movementId").value(120))
                .andExpect(jsonPath("$.amount").value(200.00))
                .andExpect(jsonPath("$.available").value(800.00))
                .andExpect(jsonPath("$.total").value(800.00))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    void unRetiroSinFondosResponde422() throws Exception {
        when(solicitarRetiro.ejecutar(any(), any(), any(Actor.class)))
                .thenThrow(new FondosInsuficientesException("Fondos insuficientes"));

        mockMvc.perform(post("/api/v1/funds/withdrawals")
                        .header("Idempotency-Key", "retiro-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":5,\"amount\":200.00}")
                        .with(autenticado(5L, "INVERSIONISTA")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void elDuenoConsultaSusMovimientosConFiltro() throws Exception {
        MovimientoResponse movimiento = new MovimientoResponse(120L, "RETIRO", new BigDecimal("200.00"),
                new BigDecimal("800.00"), "RETIRO", null, AHORA);
        when(listarMovimientos.ejecutar(eq(5L), any(FiltroMovimientos.class), any(Pageable.class),
                any(Actor.class)))
                .thenReturn(new PageImpl<>(List.of(movimiento), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/accounts/5/movements")
                        .param("filter[tipo]", "RETIRO")
                        .with(autenticado(5L, "INVERSIONISTA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].movementId").value(120))
                .andExpect(jsonPath("$.content[0].type").value("RETIRO"))
                .andExpect(jsonPath("$.content[0].balanceAfter").value(800.00))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.size").value(20));

        ArgumentCaptor<FiltroMovimientos> filtroCaptor = ArgumentCaptor.forClass(FiltroMovimientos.class);
        ArgumentCaptor<Pageable> paginaCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(listarMovimientos).ejecutar(eq(5L), filtroCaptor.capture(), paginaCaptor.capture(),
                any(Actor.class));
        assertThat(filtroCaptor.getValue().tipo()).isEqualTo(TipoMovimiento.RETIRO);
        assertThat(paginaCaptor.getValue().getSort())
                .isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Test
    void unTipoDeMovimientoInvalidoResponde400() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/5/movements")
                        .param("filter[tipo]", "INEXISTENTE")
                        .with(autenticado(5L, "INVERSIONISTA")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void unOrdenInvalidoResponde400() throws Exception {
        when(listarMovimientos.ejecutar(eq(5L), any(FiltroMovimientos.class), any(Pageable.class),
                any(Actor.class)))
                .thenThrow(new OrdenInvalidoException("Orden no permitido: descripcion"));

        mockMvc.perform(get("/api/v1/accounts/5/movements")
                        .param("sort", "descripcion")
                        .with(autenticado(5L, "INVERSIONISTA")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void elTamanoDePaginaSeRecortaACien() throws Exception {
        when(listarMovimientos.ejecutar(eq(5L), any(FiltroMovimientos.class), any(Pageable.class),
                any(Actor.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/accounts/5/movements")
                        .param("size", "500")
                        .with(autenticado(5L, "INVERSIONISTA")))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(listarMovimientos).ejecutar(eq(5L), any(FiltroMovimientos.class), captor.capture(),
                any(Actor.class));
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
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

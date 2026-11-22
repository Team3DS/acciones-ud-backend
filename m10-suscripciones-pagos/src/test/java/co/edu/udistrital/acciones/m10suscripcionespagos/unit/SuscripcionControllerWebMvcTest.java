package co.edu.udistrital.acciones.m10suscripcionespagos.unit;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.EntitlementResponse;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.MiSuscripcionResponse;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.PlanResponse;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.PrecioPlanResponse;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.ResultadoCheckout;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.SuscripcionResponse;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.RecursoNoEncontradoException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.SuscripcionAjenaException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.SuscripcionVigenteException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.usecase.CambiarIntervaloUseCase;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.usecase.CancelarSuscripcionUseCase;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.usecase.ConsultarEntitlementUseCase;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.usecase.ConsultarMiSuscripcionUseCase;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.usecase.IniciarSuscripcionUseCase;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.usecase.ListarPlanesUseCase;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.DatosSuscripcion;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.EstadoSuscripcion;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.IntervaloPlan;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.PrecioPlan;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.Plan;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.SesionCheckout;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.Suscripcion;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.adapter.in.rest.EntitlementController;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.adapter.in.rest.PlanController;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.adapter.in.rest.SuscripcionController;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.config.GlobalExceptionHandler;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security.EscritorProblemas;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security.UsuarioAutenticado;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SuscripcionControllerWebMvcTest {

    private static final long USUARIO = 4101L;
    private static final Instant INICIO = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant FIN = Instant.parse("2026-02-01T00:00:00Z");

    private final ListarPlanesUseCase listarPlanes = mock(ListarPlanesUseCase.class);
    private final ConsultarMiSuscripcionUseCase miSuscripcion = mock(ConsultarMiSuscripcionUseCase.class);
    private final ConsultarEntitlementUseCase entitlement = mock(ConsultarEntitlementUseCase.class);
    private final IniciarSuscripcionUseCase iniciarSuscripcion = mock(IniciarSuscripcionUseCase.class);
    private final CancelarSuscripcionUseCase cancelarSuscripcion = mock(CancelarSuscripcionUseCase.class);
    private final CambiarIntervaloUseCase cambiarIntervalo = mock(CambiarIntervaloUseCase.class);

    private final ObjectMapper json = Jackson2ObjectMapperBuilder.json()
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(
                    new PlanController(listarPlanes),
                    new SuscripcionController(miSuscripcion, iniciarSuscripcion,
                            cancelarSuscripcion, cambiarIntervalo),
                    new EntitlementController(entitlement))
            .setControllerAdvice(new GlobalExceptionHandler(
                    new EscritorProblemas(json, Clock.fixed(INICIO, ZoneOffset.UTC))))
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .setMessageConverters(new MappingJackson2HttpMessageConverter(json))
            .build();

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void elCatalogoEsPublicoYDevuelveLosDosPrecios() throws Exception {
        when(listarPlanes.ejecutar()).thenReturn(List.of(new PlanResponse(
                "PREMIUM", "Acciones UD Premium", "Watchlist y alertas de precio",
                List.of(
                        new PrecioPlanResponse(IntervaloPlan.MENSUAL, new BigDecimal("12.00"), "USD"),
                        new PrecioPlanResponse(IntervaloPlan.ANUAL, new BigDecimal("120.00"), "USD")))));

        mockMvc.perform(get("/api/v1/payments/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("PREMIUM"))
                .andExpect(jsonPath("$[0].prices[0].interval").value("MENSUAL"))
                .andExpect(jsonPath("$[0].prices[0].amount").value(12.00))
                .andExpect(jsonPath("$[0].prices[1].interval").value("ANUAL"))
                .andExpect(jsonPath("$[0].prices[1].amount").value(120.00));
    }

    @Test
    void miSuscripcionDevuelveElEstadoDelUsuario() throws Exception {
        when(miSuscripcion.ejecutar(USUARIO)).thenReturn(new MiSuscripcionResponse(
                USUARIO, true, FIN,
                new SuscripcionResponse(7L, "Acciones UD Premium", IntervaloPlan.MENSUAL,
                        new BigDecimal("12.00"), "USD", EstadoSuscripcion.ACTIVA,
                        INICIO, FIN, false, null, null)));

        mockMvc.perform(get("/api/v1/subscriptions/me").with(autenticado()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USUARIO))
                .andExpect(jsonPath("$.premium").value(true))
                .andExpect(jsonPath("$.validUntil").value("2026-02-01T00:00:00Z"))
                .andExpect(jsonPath("$.subscription.id").value(7))
                .andExpect(jsonPath("$.subscription.plan").value("Acciones UD Premium"))
                .andExpect(jsonPath("$.subscription.interval").value("MENSUAL"))
                .andExpect(jsonPath("$.subscription.status").value("ACTIVA"))
                .andExpect(jsonPath("$.subscription.cancelAtPeriodEnd").value(false));
    }

    @Test
    void miSuscripcionSinHistorialDevuelveSubscriptionNula() throws Exception {
        when(miSuscripcion.ejecutar(USUARIO))
                .thenReturn(new MiSuscripcionResponse(USUARIO, false, null, null));

        mockMvc.perform(get("/api/v1/subscriptions/me").with(autenticado()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.premium").value(false))
                .andExpect(jsonPath("$.validUntil").value(nullValue()))
                .andExpect(jsonPath("$.subscription").value(nullValue()));
    }

    @Test
    void elEntitlementDevuelveElContratoDeM8() throws Exception {
        when(entitlement.ejecutar(USUARIO)).thenReturn(new EntitlementResponse(true, FIN));

        mockMvc.perform(get("/internal/subscriptions/{userId}/entitlement", USUARIO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.validUntil").value("2026-02-01T00:00:00Z"));
    }

    @Test
    void elEntitlementSinPremiumDevuelveValidUntilNulo() throws Exception {
        when(entitlement.ejecutar(USUARIO)).thenReturn(new EntitlementResponse(false, null));

        mockMvc.perform(get("/internal/subscriptions/{userId}/entitlement", USUARIO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.validUntil").value(nullValue()));
    }

    @Test
    void iniciarCreaLaSesionYResponde201() throws Exception {
        when(iniciarSuscripcion.ejecutar(eq(USUARIO), eq("clave-1"), eq(IntervaloPlan.MENSUAL)))
                .thenReturn(new ResultadoCheckout(sesion("cs_test_201", "https://stripe.test/cs_201"), false));

        mockMvc.perform(post("/api/v1/subscriptions")
                        .with(autenticado())
                        .header("Idempotency-Key", "clave-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interval\":\"MENSUAL\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value("cs_test_201"))
                .andExpect(jsonPath("$.checkoutUrl").value("https://stripe.test/cs_201"))
                .andExpect(jsonPath("$.expiresAt").value("2026-02-01T00:00:00Z"));
    }

    @Test
    void iniciarQueReutilizaLaSesionResponde200() throws Exception {
        when(iniciarSuscripcion.ejecutar(eq(USUARIO), eq("clave-1"), eq(IntervaloPlan.MENSUAL)))
                .thenReturn(new ResultadoCheckout(sesion("cs_test_reuso", "https://stripe.test/reuso"), true));

        mockMvc.perform(post("/api/v1/subscriptions")
                        .with(autenticado())
                        .header("Idempotency-Key", "clave-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interval\":\"MENSUAL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("cs_test_reuso"));
    }

    @Test
    void iniciarSinHeaderDeIdempotenciaResponde400() throws Exception {
        mockMvc.perform(post("/api/v1/subscriptions")
                        .with(autenticado())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interval\":\"MENSUAL\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void iniciarSinIntervaloResponde400() throws Exception {
        mockMvc.perform(post("/api/v1/subscriptions")
                        .with(autenticado())
                        .header("Idempotency-Key", "clave-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void iniciarConSuscripcionVigenteResponde409() throws Exception {
        when(iniciarSuscripcion.ejecutar(any(), any(), any()))
                .thenThrow(new SuscripcionVigenteException("El usuario ya tiene una suscripcion vigente"));

        mockMvc.perform(post("/api/v1/subscriptions")
                        .with(autenticado())
                        .header("Idempotency-Key", "clave-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interval\":\"ANUAL\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SUSCRIPCION_VIGENTE"));
    }

    @Test
    void cancelarProgramaLaCancelacionYResponde200() throws Exception {
        when(cancelarSuscripcion.ejecutar(USUARIO, 7L))
                .thenReturn(suscripcion(IntervaloPlan.MENSUAL, true));

        mockMvc.perform(post("/api/v1/subscriptions/7/cancel").with(autenticado()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.status").value("ACTIVA"))
                .andExpect(jsonPath("$.cancelAtPeriodEnd").value(true));
    }

    @Test
    void cancelarDeOtroUsuarioResponde403() throws Exception {
        when(cancelarSuscripcion.ejecutar(any(), eq(7L)))
                .thenThrow(new SuscripcionAjenaException("La suscripcion no pertenece al usuario"));

        mockMvc.perform(post("/api/v1/subscriptions/7/cancel").with(autenticado()))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancelarInexistenteResponde404() throws Exception {
        when(cancelarSuscripcion.ejecutar(any(), eq(99L)))
                .thenThrow(new RecursoNoEncontradoException("La suscripcion no existe"));

        mockMvc.perform(post("/api/v1/subscriptions/99/cancel").with(autenticado()))
                .andExpect(status().isNotFound());
    }

    @Test
    void cambiarIntervaloResponde200ConElNuevoIntervalo() throws Exception {
        when(cambiarIntervalo.ejecutar(USUARIO, 7L, IntervaloPlan.ANUAL))
                .thenReturn(suscripcion(IntervaloPlan.ANUAL, false));

        mockMvc.perform(post("/api/v1/subscriptions/7/change-plan")
                        .with(autenticado())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interval\":\"ANUAL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interval").value("ANUAL"))
                .andExpect(jsonPath("$.amount").value(12.00));
    }

    @Test
    void cambiarIntervaloSinIntervaloResponde400() throws Exception {
        mockMvc.perform(post("/api/v1/subscriptions/7/change-plan")
                        .with(autenticado())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    private static Suscripcion suscripcion(IntervaloPlan intervalo, boolean cancelarAlFinal) {
        Plan plan = Plan.crear("PREMIUM", "Acciones UD Premium", "Watchlist y alertas");
        PrecioPlan precio = PrecioPlan.crear(plan, intervalo, new BigDecimal("12.00"),
                "USD", "acciones_ud_premium_mensual");
        Suscripcion suscripcion = Suscripcion.crear(USUARIO, new DatosSuscripcion(
                "sub_1", precio, EstadoSuscripcion.ACTIVA, INICIO, FIN, cancelarAlFinal, null, null), INICIO);
        try {
            java.lang.reflect.Field campo = Suscripcion.class.getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(suscripcion, 7L);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
        return suscripcion;
    }

    private static SesionCheckout sesion(String stripeSessionId, String url) {
        Plan plan = Plan.crear("PREMIUM", "Acciones UD Premium", "Watchlist y alertas");
        PrecioPlan precio = PrecioPlan.crear(plan, IntervaloPlan.MENSUAL, new BigDecimal("12.00"),
                "USD", "acciones_ud_premium_mensual");
        return SesionCheckout.crear(USUARIO, precio, stripeSessionId, url, "clave-1", INICIO, FIN);
    }

    private static RequestPostProcessor autenticado() {
        UsuarioAutenticado usuario = new UsuarioAutenticado(USUARIO, Set.of("INVERSIONISTA"));
        Authentication autenticacion = UsernamePasswordAuthenticationToken.authenticated(
                usuario, null, List.of(new SimpleGrantedAuthority("ROLE_INVERSIONISTA")));
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(autenticacion);
            return request;
        };
    }
}

package co.edu.udistrital.acciones.m10suscripcionespagos.unit;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.ResultadoAgregado;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.WatchlistItemResponse;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.PremiumRequeridoException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.usecase.AgregarAWatchlistUseCase;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.usecase.ConsultarWatchlistUseCase;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.usecase.QuitarDeWatchlistUseCase;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.Simbolo;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.WatchlistItem;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.adapter.in.rest.WatchlistController;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WatchlistControllerWebMvcTest {

    private static final long USUARIO = 99104L;
    private static final Instant INICIO = Instant.parse("2026-11-22T00:00:00Z");

    private final ConsultarWatchlistUseCase consultar = mock(ConsultarWatchlistUseCase.class);
    private final AgregarAWatchlistUseCase agregar = mock(AgregarAWatchlistUseCase.class);
    private final QuitarDeWatchlistUseCase quitar = mock(QuitarDeWatchlistUseCase.class);

    private final ObjectMapper json = Jackson2ObjectMapperBuilder.json()
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new WatchlistController(consultar, agregar, quitar))
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
    void listaLosItemsConSuPrecio() throws Exception {
        when(consultar.ejecutar(USUARIO)).thenReturn(List.of(new WatchlistItemResponse(
                "AAPL", INICIO, new BigDecimal("200.00"), new BigDecimal("1.00"),
                new BigDecimal("0.50"), INICIO, false)));

        mockMvc.perform(get("/api/v1/watchlist").with(autenticado()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$[0].price").value(200.00))
                .andExpect(jsonPath("$[0].stale").value(false));
    }

    @Test
    void agregarUnItemNuevoResponde201() throws Exception {
        WatchlistItem item = WatchlistItem.crear(USUARIO, Simbolo.desde("AAPL"), INICIO);
        when(agregar.ejecutar(eq(USUARIO), any()))
                .thenReturn(new ResultadoAgregado(item, true));

        mockMvc.perform(post("/api/v1/watchlist/items").with(autenticado())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symbol\":\"aapl\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.addedAt").value("2026-11-22T00:00:00Z"));
    }

    @Test
    void agregarUnItemRepetidoResponde200() throws Exception {
        WatchlistItem item = WatchlistItem.crear(USUARIO, Simbolo.desde("AAPL"), INICIO);
        when(agregar.ejecutar(eq(USUARIO), any()))
                .thenReturn(new ResultadoAgregado(item, false));

        mockMvc.perform(post("/api/v1/watchlist/items").with(autenticado())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symbol\":\"AAPL\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void unSymbolVacioResponde400() throws Exception {
        mockMvc.perform(post("/api/v1/watchlist/items").with(autenticado())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symbol\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sinPremiumLaConsultaResponde403ConCodigo() throws Exception {
        when(consultar.ejecutar(USUARIO)).thenThrow(new PremiumRequeridoException());

        mockMvc.perform(get("/api/v1/watchlist").with(autenticado()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PREMIUM_REQUERIDO"));
    }

    @Test
    void quitarUnItemResponde204() throws Exception {
        mockMvc.perform(delete("/api/v1/watchlist/items/aapl").with(autenticado()))
                .andExpect(status().isNoContent());

        verify(quitar).ejecutar(USUARIO, "aapl");
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

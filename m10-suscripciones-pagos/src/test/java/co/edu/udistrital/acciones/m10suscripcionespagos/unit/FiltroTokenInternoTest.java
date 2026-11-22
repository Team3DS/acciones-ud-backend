package co.edu.udistrital.acciones.m10suscripcionespagos.unit;

import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security.EscritorProblemas;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security.FiltroTokenInterno;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security.TokenInternoProperties;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security.UsuarioAutenticado;
import co.edu.udistrital.acciones.m10suscripcionespagos.support.TokenTestFactory;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class FiltroTokenInternoTest {

    private static final String RUTA = "/internal/subscriptions/7/entitlement";
    private static final String AUTORIDAD_ESPERADA =
            "ROLE_" + UsuarioAutenticado.ROL_SERVICIO_INTERNO;

    private final EscritorProblemas escritor = new EscritorProblemas(
            Jackson2ObjectMapperBuilder.json().build(),
            Clock.fixed(Instant.parse("2026-05-06T07:08:09Z"), ZoneOffset.UTC));
    private final FiltroTokenInterno filtro =
            new FiltroTokenInterno(new TokenInternoProperties(TokenTestFactory.TOKEN_INTERNO), escritor);

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void aceptaElTokenCorrectoYAutenticaConRolDeServicioInterno() throws Exception {
        MockHttpServletRequest request = peticion(RUTA, "Bearer " + TokenTestFactory.TOKEN_INTERNO);
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] llamado = {false};
        FilterChain cadena = (req, res) -> llamado[0] = true;

        filtro.doFilter(request, response, cadena);

        assertThat(llamado[0]).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(AUTORIDAD_ESPERADA);
    }

    @Test
    void aceptaElEsquemaBearerEnMinusculas() throws Exception {
        MockHttpServletRequest request = peticion(RUTA, "bearer " + TokenTestFactory.TOKEN_INTERNO);
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] llamado = {false};

        filtro.doFilter(request, response, (req, res) -> llamado[0] = true);

        assertThat(llamado[0]).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void rechazaUnTokenDistinto() throws Exception {
        MockHttpServletRequest request = peticion(RUTA, "Bearer " + "f".repeat(64));
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] llamado = {false};

        filtro.doFilter(request, response, (req, res) -> llamado[0] = true);

        assertThat(llamado[0]).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void rechazaUnJwtDeUsuario() throws Exception {
        MockHttpServletRequest request = peticion(RUTA, "Bearer " + TokenTestFactory.usuario());
        MockHttpServletResponse response = new MockHttpServletResponse();

        filtro.doFilter(request, response, (req, res) -> { });

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void rechazaLaPeticionSinCabecera() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", RUTA);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filtro.doFilter(request, response, (req, res) -> { });

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void rechazaTodoCuandoLaConfiguracionEstaVacia() throws Exception {
        FiltroTokenInterno sinConfigurar = new FiltroTokenInterno(new TokenInternoProperties(""), escritor);
        MockHttpServletRequest request = peticion(RUTA, "Bearer " + TokenTestFactory.TOKEN_INTERNO);
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] llamado = {false};

        sinConfigurar.doFilter(request, response, (req, res) -> llamado[0] = true);

        assertThat(llamado[0]).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void noActuaSobreRutasDeUsuario() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/subscriptions/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] llamado = {false};

        filtro.doFilter(request, response, (req, res) -> llamado[0] = true);

        assertThat(llamado[0]).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private static MockHttpServletRequest peticion(String ruta, String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", ruta);
        request.addHeader(HttpHeaders.AUTHORIZATION, authorization);
        return request;
    }
}

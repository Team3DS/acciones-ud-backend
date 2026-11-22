package co.edu.udistrital.acciones.m10suscripcionespagos.unit;

import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security.FiltroJwt;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security.UsuarioAutenticado;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security.VerificadorJwt;
import co.edu.udistrital.acciones.m10suscripcionespagos.support.TokenTestFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class FiltroJwtTest {

    private final FiltroJwt filtro = new FiltroJwt(new VerificadorJwt(TokenTestFactory.propiedades()));

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void autenticaUnTokenValidoConSusRoles() throws Exception {
        MockHttpServletRequest request = peticion("/api/v1/subscriptions/me", "Bearer " + TokenTestFactory.usuario());

        filtro.doFilter(request, new MockHttpServletResponse(), (req, res) -> { });

        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        assertThat(autenticacion).isNotNull();
        assertThat(autenticacion.getPrincipal()).isInstanceOf(UsuarioAutenticado.class);
        assertThat(((UsuarioAutenticado) autenticacion.getPrincipal()).userId())
                .isEqualTo(TokenTestFactory.USUARIO);
        assertThat(autenticacion.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_INVERSIONISTA");
    }

    @Test
    void aceptaElEsquemaBearerEnMinusculas() throws Exception {
        MockHttpServletRequest request = peticion("/api/v1/subscriptions/me", "bearer " + TokenTestFactory.usuario());

        filtro.doFilter(request, new MockHttpServletResponse(), (req, res) -> { });

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void noAutenticaSinToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/subscriptions/me");

        filtro.doFilter(request, new MockHttpServletResponse(), (req, res) -> { });

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void noAutenticaUnTokenInvalido() throws Exception {
        MockHttpServletRequest request = peticion("/api/v1/subscriptions/me", "Bearer " + TokenTestFactory.malformado());

        filtro.doFilter(request, new MockHttpServletResponse(), (req, res) -> { });

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void noActuaSobreLasRutasInternas() throws Exception {
        MockHttpServletRequest request = peticion(
                "/internal/subscriptions/1/entitlement", "Bearer " + TokenTestFactory.usuario());

        filtro.doFilter(request, new MockHttpServletResponse(), (req, res) -> { });

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private static MockHttpServletRequest peticion(String ruta, String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", ruta);
        request.addHeader(HttpHeaders.AUTHORIZATION, authorization);
        return request;
    }
}

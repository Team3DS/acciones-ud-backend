package co.edu.udistrital.acciones.m2cuentasfondos.unit;

import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.security.AuthenticatedUser;
import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.security.JwtAuthenticationFilter;
import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.security.JwtTokenValidator;
import co.edu.udistrital.acciones.m2cuentasfondos.support.TokenTestFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {

    private static final String RUTA = "/api/v1/accounts/1/balance";

    private final JwtAuthenticationFilter filtro =
            new JwtAuthenticationFilter(new JwtTokenValidator(TokenTestFactory.propiedades()));

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void autenticaConUnTokenValido() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", RUTA);
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + TokenTestFactory.token(42, "INVERSIONISTA"));
        AtomicReference<Authentication> capturada = new AtomicReference<>();

        filtro.doFilter(request, new MockHttpServletResponse(),
                (req, res) -> capturada.set(SecurityContextHolder.getContext().getAuthentication()));

        assertThat(capturada.get()).isNotNull();
        assertThat(capturada.get().getPrincipal()).isEqualTo(new AuthenticatedUser(42L, Set.of("INVERSIONISTA")));
        assertThat(capturada.get().getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_INVERSIONISTA");
    }

    @Test
    void noAutenticaSinToken() throws Exception {
        AtomicReference<Authentication> capturada = new AtomicReference<>();

        filtro.doFilter(new MockHttpServletRequest("GET", RUTA), new MockHttpServletResponse(),
                (req, res) -> capturada.set(SecurityContextHolder.getContext().getAuthentication()));

        assertThat(capturada.get()).isNull();
    }

    @Test
    void noAutenticaConUnTokenInvalido() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", RUTA);
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + TokenTestFactory.tokenMalformado());
        AtomicReference<Authentication> capturada = new AtomicReference<>();

        filtro.doFilter(request, new MockHttpServletResponse(),
                (req, res) -> capturada.set(SecurityContextHolder.getContext().getAuthentication()));

        assertThat(capturada.get()).isNull();
    }

    @Test
    void noAutenticaSiLaCabeceraNoEsBearer() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", RUTA);
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic dXN1YXJpbzpjbGF2ZQ==");
        AtomicReference<Authentication> capturada = new AtomicReference<>();

        filtro.doFilter(request, new MockHttpServletResponse(),
                (req, res) -> capturada.set(SecurityContextHolder.getContext().getAuthentication()));

        assertThat(capturada.get()).isNull();
    }

    @Test
    void noAutenticaConBearerVacio() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", RUTA);
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer   ");
        AtomicReference<Authentication> capturada = new AtomicReference<>();

        filtro.doFilter(request, new MockHttpServletResponse(),
                (req, res) -> capturada.set(SecurityContextHolder.getContext().getAuthentication()));

        assertThat(capturada.get()).isNull();
    }

    @Test
    void respetaUnaAutenticacionYaEstablecida() throws Exception {
        Authentication previa = new UsernamePasswordAuthenticationToken("previa", null, Set.of());
        SecurityContextHolder.getContext().setAuthentication(previa);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", RUTA);
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + TokenTestFactory.token(99));
        AtomicReference<Authentication> capturada = new AtomicReference<>();

        filtro.doFilter(request, new MockHttpServletResponse(),
                (req, res) -> capturada.set(SecurityContextHolder.getContext().getAuthentication()));

        assertThat(capturada.get()).isSameAs(previa);
    }
}

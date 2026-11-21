package co.edu.udistrital.acciones.m6portafolio.unit;

import co.edu.udistrital.acciones.m6portafolio.infrastructure.security.AuthenticatedUser;
import co.edu.udistrital.acciones.m6portafolio.infrastructure.security.JwtAuthenticationFilter;
import co.edu.udistrital.acciones.m6portafolio.infrastructure.security.JwtTokenValidator;
import co.edu.udistrital.acciones.m6portafolio.support.TokenTestFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {

    private static final String RUTA = "/api/v1/positions/4101";

    private final JwtAuthenticationFilter filtro =
            new JwtAuthenticationFilter(new JwtTokenValidator(TokenTestFactory.propiedades()));

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void autenticaConUnTokenValidoYConstruyeLasAutoridades() throws Exception {
        MockHttpServletRequest request = peticion("Bearer " + TokenTestFactory.administrador());

        filtro.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        assertThat(autenticacion).isNotNull();
        assertThat(autenticacion.getPrincipal())
                .isEqualTo(new AuthenticatedUser(TokenTestFactory.ADMINISTRADOR, Set.of("ADMINISTRADOR")));
        assertThat(autenticacion.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMINISTRADOR");
    }

    @Test
    void noDuplicaElPrefijoRoleDeLasAutoridades() throws Exception {
        MockHttpServletRequest request = peticion("Bearer " + TokenTestFactory.token(9, "ROLE_SISTEMA"));

        filtro.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_SISTEMA");
    }

    @Test
    void aceptaElEsquemaBearerSinDistinguirMayusculas() throws Exception {
        MockHttpServletRequest request = peticion("bearer " + TokenTestFactory.usuario());

        filtro.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void noAutenticaSinCabecera() throws Exception {
        filtro.doFilter(new MockHttpServletRequest("GET", RUTA),
                new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Basic dXN1YXJpbzpjbGF2ZQ==",
            "Bearer    ",
            "Bearer ",
            "Token abc.def.ghi"
    })
    void noAutenticaConCabecerasNoUtilizables(String cabecera) throws Exception {
        MockHttpServletRequest request = peticion(cabecera);

        filtro.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void noAutenticaConUnTokenMalformado() throws Exception {
        MockHttpServletRequest request = peticion("Bearer " + TokenTestFactory.malformado());

        filtro.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void noAutenticaConUnTokenExpirado() throws Exception {
        MockHttpServletRequest request = peticion("Bearer " + TokenTestFactory.expirado(1));

        filtro.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void conservaLaAutenticacionYaEstablecida() throws Exception {
        Authentication previa = UsernamePasswordAuthenticationToken.authenticated("previa", null, Set.of());
        SecurityContextHolder.getContext().setAuthentication(previa);

        filtro.doFilter(peticion("Bearer " + TokenTestFactory.token(99, "INVERSIONISTA")),
                new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(previa);
    }

    private MockHttpServletRequest peticion(String autorizacion) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", RUTA);
        request.addHeader(HttpHeaders.AUTHORIZATION, autorizacion);
        return request;
    }
}

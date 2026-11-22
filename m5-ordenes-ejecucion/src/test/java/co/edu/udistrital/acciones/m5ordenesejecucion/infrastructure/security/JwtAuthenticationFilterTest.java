package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenValidator validador;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain cadena;

    @Mock
    private SecurityContext securityContext;

    private JwtAuthenticationFilter filtro;
    private Authentication capturedAuthentication;

    @BeforeEach
    void setUp() {
        filtro = new JwtAuthenticationFilter(validador);
        capturedAuthentication = null;
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void configurarSecurityContext() {
        doAnswer(invocation -> {
            capturedAuthentication = invocation.getArgument(0);
            return null;
        }).when(securityContext).setAuthentication(any());
        when(securityContext.getAuthentication()).thenAnswer(invocation -> capturedAuthentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void doFilterInternalSinCabecera() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getHeader("X-User-Id")).thenReturn(null);

        filtro.doFilterInternal(request, response, cadena);

        verify(cadena).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternalConBearerTokenValido() throws ServletException, IOException {
        configurarSecurityContext();
        AuthenticatedUser usuario = new AuthenticatedUser(1L, Set.of("ADMIN"));
        when(request.getHeader("Authorization")).thenReturn("Bearer token-valido");
        when(validador.validar("token-valido")).thenReturn(usuario);

        filtro.doFilterInternal(request, response, cadena);

        verify(cadena).doFilter(request, response);
        assertThat(capturedAuthentication).isNotNull();
        assertThat(capturedAuthentication.getPrincipal()).isEqualTo(usuario);
    }

    @Test
    void doFilterInternalConBearerTokenInvalido() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer token-invalido");
        when(validador.validar("token-invalido")).thenThrow(new JwtException("Token invalido"));

        filtro.doFilterInternal(request, response, cadena);

        verify(cadena).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternalConXUserId() throws ServletException, IOException {
        configurarSecurityContext();
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getHeader("X-User-Id")).thenReturn("42");
        when(request.getHeader("X-User-Roles")).thenReturn("USER,ADMIN");

        filtro.doFilterInternal(request, response, cadena);

        verify(cadena).doFilter(request, response);
        assertThat(capturedAuthentication).isNotNull();
        AuthenticatedUser usuario = (AuthenticatedUser) capturedAuthentication.getPrincipal();
        assertThat(usuario.userId()).isEqualTo(42L);
        assertThat(usuario.roles()).containsExactlyInAnyOrder("USER", "ADMIN");
    }

    @Test
    void doFilterInternalYaAutenticado() throws ServletException, IOException {
        AuthenticatedUser usuarioExistente = new AuthenticatedUser(99L, Set.of("USER"));
        var autenticacionExistente = UsernamePasswordAuthenticationToken.authenticated(
                usuarioExistente, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(autenticacionExistente);

        filtro.doFilterInternal(request, response, cadena);

        verify(cadena).doFilter(request, response);
        verify(validador, never()).validar(any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(autenticacionExistente);
    }
}
package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.config;

import co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.security.ForbiddenAccessHandler;
import co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.security.JwtAuthenticationFilter;
import co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.security.JwtTokenValidator;
import co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.security.UnauthorizedEntryPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    JwtTokenValidator validador;

    @Mock
    UnauthorizedEntryPoint unauthorizedEntryPoint;

    @Mock
    ForbiddenAccessHandler forbiddenAccessHandler;

    @Mock
    HttpSecurity httpSecurity;

    @Test
    void securityFilterChainExisteYConfiguraRutasPublicas() throws Exception {
        SecurityConfig config = new SecurityConfig(validador, unauthorizedEntryPoint, forbiddenAccessHandler);

        when(httpSecurity.csrf(any())).thenReturn(httpSecurity);
        when(httpSecurity.cors(any())).thenReturn(httpSecurity);
        when(httpSecurity.sessionManagement(any())).thenReturn(httpSecurity);
        when(httpSecurity.httpBasic(any())).thenReturn(httpSecurity);
        when(httpSecurity.formLogin(any())).thenReturn(httpSecurity);
        when(httpSecurity.logout(any())).thenReturn(httpSecurity);
        when(httpSecurity.authorizeHttpRequests(any())).thenReturn(httpSecurity);
        when(httpSecurity.exceptionHandling(any())).thenReturn(httpSecurity);
        when(httpSecurity.addFilterBefore(any(), any())).thenReturn(httpSecurity);
        when(httpSecurity.build()).thenReturn(mock(DefaultSecurityFilterChain.class));

        SecurityFilterChain chain = config.securityFilterChain(httpSecurity);

        assertThat(chain).isNotNull();
        verify(httpSecurity).authorizeHttpRequests(any());
        verify(httpSecurity).addFilterBefore(any(JwtAuthenticationFilter.class), any());
    }

    @Test
    void rutasPublicasIncluyenEndpointsEsperados() throws Exception {
        Field field = SecurityConfig.class.getDeclaredField("RUTAS_PUBLICAS");
        field.setAccessible(true);
        String[] rutasPublicas = (String[]) field.get(null);

        assertThat(rutasPublicas).contains(
                "/actuator/**",
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/error",
                "/api/v1/webhooks/**"
        );
    }
}
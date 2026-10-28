package com.gfc.infrastructure.security.jwt;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

@SpringBootTest
class SecurityConfigTest {

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Test
    void corsConfigurationSourceContieneOrigenesEsperados() {
        CorsConfiguration config = corsConfigurationSource.getCorsConfiguration(
                new org.springframework.mock.web.MockHttpServletRequest());
        assertTrue(config != null, "CorsConfiguration no debe ser nulo");
        List<String> origins = config.getAllowedOrigins();
        assertTrue(origins.contains("http://localhost:4321"),
                "Debe contener el origen por defecto localhost:4321");
    }

    @Test
    void corsConfigurationSourcePermiteCredenciales() {
        CorsConfiguration config = corsConfigurationSource.getCorsConfiguration(
                new org.springframework.mock.web.MockHttpServletRequest());
        assertTrue(config.getAllowCredentials() == Boolean.TRUE,
                "Debe permitir credenciales");
    }

    @Test
    void corsConfigurationSourcePermiteMetodosEsperados() {
        CorsConfiguration config = corsConfigurationSource.getCorsConfiguration(
                new org.springframework.mock.web.MockHttpServletRequest());
        List<String> methods = config.getAllowedMethods();
        assertTrue(methods.contains("GET"), "Debe permitir GET");
        assertTrue(methods.contains("POST"), "Debe permitir POST");
        assertTrue(methods.contains("PUT"), "Debe permitir PUT");
        assertTrue(methods.contains("DELETE"), "Debe permitir DELETE");
    }
}

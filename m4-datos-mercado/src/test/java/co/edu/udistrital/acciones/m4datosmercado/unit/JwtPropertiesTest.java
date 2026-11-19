package co.edu.udistrital.acciones.m4datosmercado.unit;

import co.edu.udistrital.acciones.m4datosmercado.infrastructure.security.JwtProperties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtPropertiesTest {

    @Test
    void recordConSecretValido() {
        JwtProperties props = new JwtProperties("secret-key-minimo-32-caracteres");
        assertEquals("secret-key-minimo-32-caracteres", props.secret());
    }
}
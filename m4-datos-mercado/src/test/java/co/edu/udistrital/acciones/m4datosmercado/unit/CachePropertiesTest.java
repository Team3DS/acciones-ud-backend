package co.edu.udistrital.acciones.m4datosmercado.unit;

import co.edu.udistrital.acciones.m4datosmercado.infrastructure.config.CacheProperties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CachePropertiesTest {

    @Test
    void recordConValoresValidos() {
        CacheProperties props = new CacheProperties(1, 60);
        assertEquals(1, props.quoteTtlSeconds());
        assertEquals(60, props.snapshotTtlSeconds());
    }
}
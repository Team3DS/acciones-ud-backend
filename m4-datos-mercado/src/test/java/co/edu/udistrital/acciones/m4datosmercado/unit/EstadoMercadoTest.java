package co.edu.udistrital.acciones.m4datosmercado.unit;

import co.edu.udistrital.acciones.m4datosmercado.domain.EstadoMercado;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EstadoMercadoTest {

    @Test
    void valoresEnum() {
        assertEquals(4, EstadoMercado.values().length);
        assertEquals("CERRADO", EstadoMercado.CERRADO.name());
        assertEquals("PRE_MARKET", EstadoMercado.PRE_MARKET.name());
        assertEquals("ABIERTO", EstadoMercado.ABIERTO.name());
        assertEquals("AFTER_HOURS", EstadoMercado.AFTER_HOURS.name());
    }
}
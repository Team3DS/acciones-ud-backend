package co.edu.udistrital.acciones.m4datosmercado.unit;

import co.edu.udistrital.acciones.m4datosmercado.domain.Intervalo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IntervaloTest {

    @Test
    void fromStringValido() {
        assertEquals(Intervalo.M1, Intervalo.fromString("1m"));
        assertEquals(Intervalo.M5, Intervalo.fromString("5m"));
        assertEquals(Intervalo.M15, Intervalo.fromString("15m"));
        assertEquals(Intervalo.H1, Intervalo.fromString("1h"));
        assertEquals(Intervalo.D1, Intervalo.fromString("1d"));
        assertEquals(Intervalo.M1, Intervalo.fromString("1M"));
        assertEquals(Intervalo.H1, Intervalo.fromString("1H"));
    }

    @Test
    void fromStringInvalidoLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> Intervalo.fromString("30m"));
        assertThrows(IllegalArgumentException.class, () -> Intervalo.fromString("invalid"));
    }

    @Test
    void getValor() {
        assertEquals("1m", Intervalo.M1.getValor());
        assertEquals("5m", Intervalo.M5.getValor());
        assertEquals("15m", Intervalo.M15.getValor());
        assertEquals("1h", Intervalo.H1.getValor());
        assertEquals("1d", Intervalo.D1.getValor());
    }
}
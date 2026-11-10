package co.edu.udistrital.acciones.m4datosmercado.domain;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import static org.junit.jupiter.api.Assertions.*;

class HorarioMercadoTest {
    @Test
    void obtenerEstadoCerrado() {
        HorarioMercado h = new HorarioMercado();
        java.lang.reflect.Field f1;
        try {
            f1 = HorarioMercado.class.getDeclaredField("mercadoMic");
            f1.setAccessible(true);
            f1.set(h, "NYSE");
            java.lang.reflect.Field f2 = HorarioMercado.class.getDeclaredField("apertura");
            f2.setAccessible(true);
            f2.set(h, LocalTime.of(14,0));
            java.lang.reflect.Field f3 = HorarioMercado.class.getDeclaredField("cierre");
            f3.setAccessible(true);
            f3.set(h, LocalTime.of(21,0));
        } catch (Exception e) { fail(e); }
        Instant instant = Instant.parse("2026-01-01T10:00:00Z");
        assertEquals(EstadoMercado.CERRADO, h.obtenerEstado(instant));
    }
}

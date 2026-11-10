package co.edu.udistrital.acciones.m4datosmercado.domain;

import org.junit.jupiter.api.Test;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import static org.junit.jupiter.api.Assertions.*;

class HorarioMercadoTest {
    private HorarioMercado build(String mercado, LocalTime apertura, LocalTime cierre, LocalTime pre, LocalTime after) {
        HorarioMercado h = new HorarioMercado();
        set(h, "mercadoMic", mercado);
        set(h, "diaSemana", 1);
        set(h, "apertura", apertura);
        set(h, "cierre", cierre);
        set(h, "preMarket", pre);
        set(h, "afterHours", after);
        return h;
    }

    private void set(Object obj, String field, Object value) {
        try {
            var f = obj.getClass().getDeclaredField(field);
            f.setAccessible(true);
            f.set(obj, value);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test
    void obtenerEstado_abierto() {
        HorarioMercado h = build("NYSE", LocalTime.of(14,0), LocalTime.of(21,0), LocalTime.of(13,30), LocalTime.of(22,0));
        Instant instant = Instant.parse("2026-01-05T15:00:00Z"); // Día laboral
        assertEquals(EstadoMercado.ABIERTO, h.obtenerEstado(instant));
    }

    @Test
    void obtenerEstado_preMarket() {
        HorarioMercado h = build("NYSE", LocalTime.of(14,0), LocalTime.of(21,0), LocalTime.of(13,30), null);
        Instant instant = Instant.parse("2026-01-05T13:45:00Z");
        assertEquals(EstadoMercado.PRE_MARKET, h.obtenerEstado(instant));
    }

    @Test
    void obtenerEstado_afterHours() {
        HorarioMercado h = build("NYSE", LocalTime.of(14,0), LocalTime.of(21,0), null, LocalTime.of(22,0));
        Instant instant = Instant.parse("2026-01-05T21:30:00Z");
        assertEquals(EstadoMercado.AFTER_HOURS, h.obtenerEstado(instant));
    }

    @Test
    void obtenerEstado_cerrado_fueraDeHorario() {
        HorarioMercado h = build("NYSE", LocalTime.of(14,0), LocalTime.of(21,0), null, null);
        Instant instant = Instant.parse("2026-01-05T10:00:00Z");
        assertEquals(EstadoMercado.CERRADO, h.obtenerEstado(instant));
    }

    @Test
    void esDiaLaborable_sabado_false() {
        HorarioMercado h = new HorarioMercado();
        Instant sabado = Instant.parse("2026-01-10T15:00:00Z"); // 2026-01-10 es sábado
        assertFalse(h.esDiaLaborable(sabado));
    }

    @Test
    void esDiaLaborable_lunes_true() {
        HorarioMercado h = new HorarioMercado();
        Instant lunes = Instant.parse("2026-01-05T15:00:00Z");
        assertTrue(h.esDiaLaborable(lunes));
    }
}

package co.edu.udistrital.acciones.m6portafolio.unit;

import co.edu.udistrital.acciones.m6portafolio.domain.LadoEjecucion;
import co.edu.udistrital.acciones.m6portafolio.domain.MovimientoPosicion;
import co.edu.udistrital.acciones.m6portafolio.domain.TipoMovimientoPosicion;
import co.edu.udistrital.acciones.m6portafolio.domain.excepcion.CantidadInvalidaException;
import co.edu.udistrital.acciones.m6portafolio.domain.excepcion.MovimientoInvalidoException;
import co.edu.udistrital.acciones.m6portafolio.domain.excepcion.PrecioInvalidoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MovimientoPosicionTest {

    private static final Instant AHORA = Instant.parse("2026-02-03T04:05:06Z");

    @Test
    void registraUnaCompraNormalizada() {
        MovimientoPosicion movimiento = MovimientoPosicion.compra(7L, new BigDecimal("10.5"),
                new BigDecimal("180.00"), "EXE-1", "ORD-1", "EXE:EXE-1", AHORA);

        assertThat(movimiento.getPosicionId()).isEqualTo(7L);
        assertThat(movimiento.getTipo()).isEqualTo(TipoMovimientoPosicion.COMPRA);
        assertThat(movimiento.getCantidad()).isEqualByComparingTo("10.5");
        assertThat(movimiento.getCantidad().scale()).isEqualTo(8);
        assertThat(movimiento.getPrecio()).isEqualByComparingTo("180.00");
        assertThat(movimiento.getExecutionId()).isEqualTo("EXE-1");
        assertThat(movimiento.getOrderId()).isEqualTo("ORD-1");
        assertThat(movimiento.getIdempotencyKey()).isEqualTo("EXE:EXE-1");
        assertThat(movimiento.getOcurridoEn()).isEqualTo(AHORA);
    }

    @Test
    void registraUnaReservaSinEjecucion() {
        MovimientoPosicion movimiento = MovimientoPosicion.reserva(7L, new BigDecimal("10"),
                new BigDecimal("185.00"), "ORD-1", "RES:77", AHORA);

        assertThat(movimiento.getTipo()).isEqualTo(TipoMovimientoPosicion.RESERVA);
        assertThat(movimiento.getCantidad()).isEqualByComparingTo("10");
        assertThat(movimiento.getPrecio()).isEqualByComparingTo("185.00");
        assertThat(movimiento.getExecutionId()).isNull();
        assertThat(movimiento.getOrderId()).isEqualTo("ORD-1");
        assertThat(movimiento.getIdempotencyKey()).isEqualTo("RES:77");
        assertThat(movimiento.getOcurridoEn()).isEqualTo(AHORA);
    }

    @Test
    void registraUnaLiberacionSinEjecucion() {
        MovimientoPosicion movimiento = MovimientoPosicion.liberacion(7L, new BigDecimal("4"),
                new BigDecimal("185.00"), "ORD-1", "LIB:77", AHORA);

        assertThat(movimiento.getTipo()).isEqualTo(TipoMovimientoPosicion.LIBERACION);
        assertThat(movimiento.getCantidad()).isEqualByComparingTo("4");
        assertThat(movimiento.getExecutionId()).isNull();
        assertThat(movimiento.getIdempotencyKey()).isEqualTo("LIB:77");
    }

    @Test
    void losLadosDeEjecucionSonComprasYVentas() {
        assertThat(LadoEjecucion.values()).containsExactly(LadoEjecucion.BUY, LadoEjecucion.SELL);
        assertThat(TipoMovimientoPosicion.values())
                .containsExactly(TipoMovimientoPosicion.COMPRA, TipoMovimientoPosicion.VENTA,
                        TipoMovimientoPosicion.RESERVA, TipoMovimientoPosicion.LIBERACION,
                        TipoMovimientoPosicion.AJUSTE);
    }

    @Test
    void rechazaCantidadOPrecioInvalidos() {
        assertThatThrownBy(() -> MovimientoPosicion.compra(1L, BigDecimal.ZERO, BigDecimal.ONE,
                "EXE", "ORD", "EXE:1", AHORA))
                .isInstanceOf(CantidadInvalidaException.class);
        assertThatThrownBy(() -> MovimientoPosicion.compra(1L, BigDecimal.ONE, BigDecimal.ZERO,
                "EXE", "ORD", "EXE:1", AHORA))
                .isInstanceOf(PrecioInvalidoException.class);
    }

    @Test
    void exigePosicionYClaves() {
        assertThatThrownBy(() -> MovimientoPosicion.compra(null, BigDecimal.ONE, BigDecimal.ONE,
                "EXE", "ORD", "EXE:1", AHORA))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> MovimientoPosicion.compra(1L, BigDecimal.ONE, BigDecimal.ONE,
                " ", "ORD", "EXE:1", AHORA))
                .isInstanceOf(MovimientoInvalidoException.class);
        assertThatThrownBy(() -> MovimientoPosicion.compra(1L, BigDecimal.ONE, BigDecimal.ONE,
                "EXE", null, "EXE:1", AHORA))
                .isInstanceOf(MovimientoInvalidoException.class);
        assertThatThrownBy(() -> MovimientoPosicion.compra(1L, BigDecimal.ONE, BigDecimal.ONE,
                "EXE", "ORD", null, AHORA))
                .isInstanceOf(MovimientoInvalidoException.class);
        assertThatThrownBy(() -> MovimientoPosicion.compra(1L, BigDecimal.ONE, BigDecimal.ONE,
                "EXE", "ORD", "EXE:1", null))
                .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void exigeEjecucionYOrden(String valor) {
        assertThatThrownBy(() -> MovimientoPosicion.compra(1L, BigDecimal.ONE, BigDecimal.ONE,
                valor, valor, "EXE:1", AHORA))
                .isInstanceOf(MovimientoInvalidoException.class);
    }
}

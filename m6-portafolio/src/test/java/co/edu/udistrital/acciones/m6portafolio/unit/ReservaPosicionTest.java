package co.edu.udistrital.acciones.m6portafolio.unit;

import co.edu.udistrital.acciones.m6portafolio.domain.EstadoReservaPosicion;
import co.edu.udistrital.acciones.m6portafolio.domain.ReservaPosicion;
import co.edu.udistrital.acciones.m6portafolio.domain.excepcion.CantidadInvalidaException;
import co.edu.udistrital.acciones.m6portafolio.domain.excepcion.TransicionReservaException;
import co.edu.udistrital.acciones.m6portafolio.domain.excepcion.VentaNoDisponibleException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservaPosicionTest {

    private static final Instant T0 = Instant.parse("2026-02-03T10:00:00Z");
    private static final Instant T1 = T0.plusSeconds(30);

    @Test
    void creaUnaReservaPendienteConLoPendienteIgualALaCantidad() {
        ReservaPosicion reserva = ReservaPosicion.crear(7L, "ORD-1", new BigDecimal("10"), "RSV:clave-1", T0);

        assertThat(reserva.getPosicionId()).isEqualTo(7L);
        assertThat(reserva.getOrderId()).isEqualTo("ORD-1");
        assertThat(reserva.getCantidad()).isEqualByComparingTo("10");
        assertThat(reserva.getCantidad().scale()).isEqualTo(8);
        assertThat(reserva.getCantidadEjecutada()).isEqualByComparingTo("0");
        assertThat(reserva.getEstado()).isEqualTo(EstadoReservaPosicion.PENDING);
        assertThat(reserva.getIdempotencyKey()).isEqualTo("RSV:clave-1");
        assertThat(reserva.pendiente()).isEqualByComparingTo("10");
        assertThat(reserva.getMotivoLiberacion()).isNull();
        assertThat(reserva.getCreadoEn()).isEqualTo(T0);
        assertThat(reserva.getConfirmadoEn()).isNull();
        assertThat(reserva.getLiberadoEn()).isNull();
    }

    @Test
    void liberarDevuelveLoPendienteYRegistraElMotivo() {
        ReservaPosicion reserva = reserva();

        BigDecimal pendiente = reserva.liberar("cancelada por el usuario", T1);

        assertThat(pendiente).isEqualByComparingTo("10");
        assertThat(reserva.getEstado()).isEqualTo(EstadoReservaPosicion.RELEASED);
        assertThat(reserva.getMotivoLiberacion()).isEqualTo("cancelada por el usuario");
        assertThat(reserva.getLiberadoEn()).isEqualTo(T1);
    }

    @Test
    void noSeLiberaDosVeces() {
        ReservaPosicion reserva = reserva();
        reserva.liberar("cancelada", T1);

        assertThatThrownBy(() -> reserva.liberar("otra vez", T1))
                .isInstanceOf(TransicionReservaException.class)
                .hasMessageContaining("RELEASED");
    }

    @Test
    void ejecucionesParcialesAcumulanHastaConfirmarLaReserva() {
        ReservaPosicion reserva = reserva();

        reserva.ejecutar(new BigDecimal("4"), T1);

        assertThat(reserva.getCantidadEjecutada()).isEqualByComparingTo("4");
        assertThat(reserva.pendiente()).isEqualByComparingTo("6");
        assertThat(reserva.getEstado()).isEqualTo(EstadoReservaPosicion.PENDING);
        reserva.ejecutar(new BigDecimal("6"), T1.plusSeconds(1));

        assertThat(reserva.getCantidadEjecutada()).isEqualByComparingTo("10");
        assertThat(reserva.pendiente()).isEqualByComparingTo("0");
        assertThat(reserva.getEstado()).isEqualTo(EstadoReservaPosicion.CONFIRMED);
        assertThat(reserva.getConfirmadoEn()).isEqualTo(T1.plusSeconds(1));
    }

    @Test
    void noEjecutaMasDeLoPendienteNiUnaReservaLiberada() {
        ReservaPosicion reserva = reserva();

        assertThatThrownBy(() -> reserva.ejecutar(new BigDecimal("11"), T1))
                .isInstanceOf(VentaNoDisponibleException.class);
        reserva.liberar("cancelada", T1);
        assertThatThrownBy(() -> reserva.ejecutar(BigDecimal.ONE, T1.plusSeconds(1)))
                .isInstanceOf(TransicionReservaException.class);
    }

    @Test
    void exigeLosDatosObligatorios() {
        assertThatThrownBy(() -> ReservaPosicion.crear(null, "ORD-1", BigDecimal.ONE, "RSV:1", T0))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ReservaPosicion.crear(7L, " ", BigDecimal.ONE, "RSV:1", T0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ReservaPosicion.crear(7L, "ORD-1", BigDecimal.ZERO, "RSV:1", T0))
                .isInstanceOf(CantidadInvalidaException.class);
        assertThatThrownBy(() -> ReservaPosicion.crear(7L, "ORD-1", BigDecimal.ONE, null, T0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ReservaPosicion.crear(7L, "ORD-1", BigDecimal.ONE, "RSV:1", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void exigeElInstanteAlLiberar() {
        ReservaPosicion reserva = reserva();

        assertThatThrownBy(() -> reserva.liberar("cancelada", null))
                .isInstanceOf(NullPointerException.class);
        assertThat(reserva.getEstado()).isEqualTo(EstadoReservaPosicion.PENDING);
    }

    private static ReservaPosicion reserva() {
        return ReservaPosicion.crear(7L, "ORD-1", new BigDecimal("10"), "RSV:clave-1", T0);
    }
}

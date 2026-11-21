package co.edu.udistrital.acciones.m6portafolio.unit;

import co.edu.udistrital.acciones.m6portafolio.domain.EstadoReservaPosicion;
import co.edu.udistrital.acciones.m6portafolio.domain.ReservaPosicion;
import co.edu.udistrital.acciones.m6portafolio.domain.excepcion.CantidadInvalidaException;
import co.edu.udistrital.acciones.m6portafolio.domain.excepcion.TransicionReservaException;
import co.edu.udistrital.acciones.m6portafolio.domain.excepcion.VentaNoDisponibleException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservaPosicionTest {

    private static final Instant T0 = Instant.parse("2026-02-03T10:00:00Z");
    private static final Instant T1 = T0.plusSeconds(30);
    private static final UUID CLAVE = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    @Test
    void creaUnaReservaActivaConLoPendienteIgualALaCantidad() {
        ReservaPosicion reserva = ReservaPosicion.crear(7L, 100L, 10, CLAVE, T0);

        assertThat(reserva.getPosicionId()).isEqualTo(7L);
        assertThat(reserva.getOrdenId()).isEqualTo(100L);
        assertThat(reserva.getCantidadReservada()).isEqualTo(10);
        assertThat(reserva.getCantidadConsumida()).isEqualTo(0);
        assertThat(reserva.getCantidadLiberada()).isEqualTo(0);
        assertThat(reserva.getEstado()).isEqualTo(EstadoReservaPosicion.ACTIVA);
        assertThat(reserva.getClaveIdempotencia()).isEqualTo(CLAVE);
        assertThat(reserva.pendiente()).isEqualTo(10);
        assertThat(reserva.getMotivoLiberacion()).isNull();
        assertThat(reserva.getCreadaEn()).isEqualTo(T0);
        assertThat(reserva.getLiberadaEn()).isNull();
    }

    @Test
    void liberarTransfiereLoPendienteACantidadLiberadaYRegistraElMotivo() {
        ReservaPosicion reserva = reserva();

        reserva.liberar("cancelada por el usuario", T1);

        assertThat(reserva.getCantidadLiberada()).isEqualTo(10);
        assertThat(reserva.getEstado()).isEqualTo(EstadoReservaPosicion.LIBERADA);
        assertThat(reserva.getMotivoLiberacion()).isEqualTo("cancelada por el usuario");
        assertThat(reserva.getLiberadaEn()).isEqualTo(T1);
        assertThat(reserva.pendiente()).isEqualTo(0);
    }

    @Test
    void noSeLiberaDosVeces() {
        ReservaPosicion reserva = reserva();
        reserva.liberar("cancelada", T1);

        assertThatThrownBy(() -> reserva.liberar("otra vez", T1))
                .isInstanceOf(TransicionReservaException.class)
                .hasMessageContaining("LIBERADA");
    }

    @Test
    void consumosParcialesAcumulanHastaConsumirLaReserva() {
        ReservaPosicion reserva = reserva();

        reserva.consumir(4);

        assertThat(reserva.getCantidadConsumida()).isEqualTo(4);
        assertThat(reserva.pendiente()).isEqualTo(6);
        assertThat(reserva.getEstado()).isEqualTo(EstadoReservaPosicion.ACTIVA);
        reserva.consumir(6);

        assertThat(reserva.getCantidadConsumida()).isEqualTo(10);
        assertThat(reserva.pendiente()).isEqualTo(0);
        assertThat(reserva.getEstado()).isEqualTo(EstadoReservaPosicion.CONSUMIDA);
    }

    @Test
    void noConsumeMasDeLoPendienteNiUnaReservaLiberada() {
        ReservaPosicion reserva = reserva();

        assertThatThrownBy(() -> reserva.consumir(11))
                .isInstanceOf(VentaNoDisponibleException.class);
        reserva.liberar("cancelada", T1);
        assertThatThrownBy(() -> reserva.consumir(1))
                .isInstanceOf(TransicionReservaException.class);
    }

    @Test
    void exigeLosDatosObligatorios() {
        assertThatThrownBy(() -> ReservaPosicion.crear(null, 100L, 10, CLAVE, T0))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ReservaPosicion.crear(7L, null, 10, CLAVE, T0))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ReservaPosicion.crear(7L, 100L, 0, CLAVE, T0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ReservaPosicion.crear(7L, 100L, -1, CLAVE, T0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ReservaPosicion.crear(7L, 100L, 10, null, T0))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ReservaPosicion.crear(7L, 100L, 10, CLAVE, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void exigeElInstanteAlLiberar() {
        ReservaPosicion reserva = reserva();

        assertThatThrownBy(() -> reserva.liberar("cancelada", null))
                .isInstanceOf(NullPointerException.class);
        assertThat(reserva.getEstado()).isEqualTo(EstadoReservaPosicion.ACTIVA);
    }

    @Test
    void exigeCantidadPositivaAlConsumir() {
        ReservaPosicion reserva = reserva();

        assertThatThrownBy(() -> reserva.consumir(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> reserva.consumir(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static ReservaPosicion reserva() {
        return ReservaPosicion.crear(7L, 100L, 10, CLAVE, T0);
    }
}
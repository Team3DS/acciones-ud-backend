package co.edu.udistrital.acciones.m6portafolio.unit;

import co.edu.udistrital.acciones.m6portafolio.domain.Posicion;
import co.edu.udistrital.acciones.m6portafolio.domain.excepcion.CantidadInvalidaException;
import co.edu.udistrital.acciones.m6portafolio.domain.excepcion.PrecioInvalidoException;
import co.edu.udistrital.acciones.m6portafolio.domain.excepcion.ReservaNoDisponibleException;
import co.edu.udistrital.acciones.m6portafolio.domain.excepcion.SimboloInvalidoException;
import co.edu.udistrital.acciones.m6portafolio.domain.excepcion.UsuarioInvalidoException;
import co.edu.udistrital.acciones.m6portafolio.domain.excepcion.VentaNoDisponibleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PosicionTest {

    private static final Long USUARIO = 4101L;
    private static final Instant T0 = Instant.parse("2026-01-02T10:00:00Z");

    @Test
    void abreUnaPosicionEnCeroYNormalizaElSimbolo() {
        Posicion posicion = Posicion.abrir(USUARIO, " aapl ", T0);

        assertThat(posicion.getUserId()).isEqualTo(USUARIO);
        assertThat(posicion.getSymbol()).isEqualTo("AAPL");
        assertThat(posicion.getCantidad()).isEqualByComparingTo("0");
        assertThat(posicion.getCantidadReservada()).isEqualByComparingTo("0");
        assertThat(posicion.getCostoPromedio()).isEqualByComparingTo("0");
        assertThat(posicion.getPrecioActual()).isNull();
        assertThat(posicion.getValoradoEn()).isNull();
        assertThat(posicion.disponible()).isEqualByComparingTo("0");
        assertThat(posicion.getCreadoEn()).isEqualTo(T0);
        assertThat(posicion.getActualizadoEn()).isEqualTo(T0);
    }

    @Test
    void dosComprasCalculanElCostoPonderadoConEscalaCuatro() {
        Posicion posicion = Posicion.abrir(USUARIO, "AAPL", T0);

        posicion.comprar(new BigDecimal("10"), new BigDecimal("180.00"), T0);
        posicion.comprar(new BigDecimal("10"), new BigDecimal("190.00"), T0.plusSeconds(60));

        assertThat(posicion.getCantidad()).isEqualByComparingTo("20");
        assertThat(posicion.getCostoPromedio()).isEqualByComparingTo("185.0000");
        assertThat(posicion.getCostoPromedio().scale()).isEqualTo(4);
        assertThat(posicion.disponible()).isEqualByComparingTo("20");
        assertThat(posicion.getPrecioActual()).isEqualByComparingTo("190.0000");
        assertThat(posicion.getValoradoEn()).isEqualTo(T0.plusSeconds(60));
        assertThat(posicion.getActualizadoEn()).isEqualTo(T0.plusSeconds(60));
    }

    @Test
    void redondeaElCostoPonderadoHaciaArriba() {
        Posicion posicion = Posicion.abrir(USUARIO, "MSFT", T0);

        posicion.comprar(BigDecimal.ONE, new BigDecimal("1.0000"), T0);
        posicion.comprar(new BigDecimal("2"), new BigDecimal("1.0001"), T0.plusSeconds(1));

        assertThat(posicion.getCostoPromedio()).isEqualByComparingTo("1.0001");
    }

    @Test
    void mantieneElPrecioMasRecienteCuandoLlegaUnaCompraAntigua() {
        Posicion posicion = Posicion.abrir(USUARIO, "AAPL", T0);
        posicion.comprar(BigDecimal.TEN, new BigDecimal("190.00"), T0.plusSeconds(120));

        posicion.comprar(BigDecimal.TEN, new BigDecimal("180.00"), T0.plusSeconds(60));

        assertThat(posicion.getCostoPromedio()).isEqualByComparingTo("185.0000");
        assertThat(posicion.getPrecioActual()).isEqualByComparingTo("190.0000");
        assertThat(posicion.getValoradoEn()).isEqualTo(T0.plusSeconds(120));
    }

    @Test
    void noReemplazaElPrecioConUnInstanteIgual() {
        Posicion posicion = Posicion.abrir(USUARIO, "AAPL", T0);
        posicion.comprar(BigDecimal.TEN, new BigDecimal("190.00"), T0);

        posicion.comprar(BigDecimal.TEN, new BigDecimal("180.00"), T0);

        assertThat(posicion.getPrecioActual()).isEqualByComparingTo("190.0000");
        assertThat(posicion.getCostoPromedio()).isEqualByComparingTo("185.0000");
    }

    @Test
    void exigeCantidadYPrecioPositivos() {
        Posicion posicion = Posicion.abrir(USUARIO, "AAPL", T0);

        assertThatThrownBy(() -> posicion.comprar(BigDecimal.ZERO, BigDecimal.ONE, T0))
                .isInstanceOf(CantidadInvalidaException.class);
        assertThatThrownBy(() -> posicion.comprar(BigDecimal.ONE, BigDecimal.ZERO, T0))
                .isInstanceOf(PrecioInvalidoException.class);
        assertThatThrownBy(() -> posicion.comprar(null, BigDecimal.ONE, T0))
                .isInstanceOf(CantidadInvalidaException.class);
        assertThatThrownBy(() -> posicion.comprar(BigDecimal.ONE, null, T0))
                .isInstanceOf(PrecioInvalidoException.class);
    }

    @Test
    void reservaReduceElDisponibleYLiberarLoRestaura() {
        Posicion posicion = Posicion.abrir(USUARIO, "AAPL", T0);
        posicion.comprar(new BigDecimal("20"), new BigDecimal("185.00"), T0);

        posicion.reservar(new BigDecimal("10"), T0.plusSeconds(10));

        assertThat(posicion.getCantidad()).isEqualByComparingTo("20");
        assertThat(posicion.getCantidadReservada()).isEqualByComparingTo("10");
        assertThat(posicion.disponible()).isEqualByComparingTo("10");
        assertThat(posicion.getActualizadoEn()).isEqualTo(T0.plusSeconds(10));

        posicion.liberarReserva(new BigDecimal("4"), T0.plusSeconds(20));

        assertThat(posicion.getCantidadReservada()).isEqualByComparingTo("6");
        assertThat(posicion.disponible()).isEqualByComparingTo("14");
        assertThat(posicion.getCostoPromedio()).isEqualByComparingTo("185.0000");
    }

    @Test
    void noReservaMasQueElDisponible() {
        Posicion posicion = Posicion.abrir(USUARIO, "AAPL", T0);
        posicion.comprar(BigDecimal.TEN, new BigDecimal("100.00"), T0);
        posicion.reservar(new BigDecimal("8"), T0);

        assertThatThrownBy(() -> posicion.reservar(new BigDecimal("3"), T0))
                .isInstanceOf(ReservaNoDisponibleException.class);
        assertThat(posicion.getCantidadReservada()).isEqualByComparingTo("8");
        assertThat(posicion.getActualizadoEn()).isEqualTo(T0);
    }

    @Test
    void noLiberaMasDeLoReservado() {
        Posicion posicion = Posicion.abrir(USUARIO, "AAPL", T0);
        posicion.comprar(BigDecimal.TEN, new BigDecimal("100.00"), T0);
        posicion.reservar(new BigDecimal("4"), T0);

        assertThatThrownBy(() -> posicion.liberarReserva(new BigDecimal("5"), T0))
                .isInstanceOf(ReservaNoDisponibleException.class);
        assertThat(posicion.getCantidadReservada()).isEqualByComparingTo("4");
    }

    @Test
    void venderReservadoCalculaGananciaYReduceLaReserva() {
        Posicion posicion = Posicion.abrir(USUARIO, "AAPL", T0);
        posicion.comprar(new BigDecimal("10"), new BigDecimal("185.00"), T0);
        posicion.reservar(new BigDecimal("6"), T0.plusSeconds(1));

        posicion.venderReservado(new BigDecimal("6"), new BigDecimal("200.00"), T0.plusSeconds(2));

        assertThat(posicion.getCantidad()).isEqualByComparingTo("4");
        assertThat(posicion.getCantidadReservada()).isEqualByComparingTo("0");
        assertThat(posicion.getCostoPromedio()).isEqualByComparingTo("185.0000");
        assertThat(posicion.getPnlRealizado()).isEqualByComparingTo("90.0000");
    }

    @Test
    void venderDisponibleCalculaPerdidaYReiniciaElCostoAlCerrarLaPosicion() {
        Posicion posicion = Posicion.abrir(USUARIO, "AAPL", T0);
        posicion.comprar(new BigDecimal("3"), new BigDecimal("10.0000"), T0);

        posicion.venderDisponible(new BigDecimal("3"), new BigDecimal("9.3333"), T0.plusSeconds(1));

        assertThat(posicion.getCantidad()).isEqualByComparingTo("0");
        assertThat(posicion.getCostoPromedio()).isEqualByComparingTo("0");
        assertThat(posicion.getPnlRealizado()).isEqualByComparingTo("-2.0001");
    }

    @Test
    void noVendeMasDeLoReservadoONoDisponible() {
        Posicion posicion = Posicion.abrir(USUARIO, "AAPL", T0);
        posicion.comprar(new BigDecimal("10"), new BigDecimal("100.00"), T0);
        posicion.reservar(new BigDecimal("6"), T0);

        assertThatThrownBy(() -> posicion.venderReservado(new BigDecimal("7"), BigDecimal.ONE, T0))
                .isInstanceOf(VentaNoDisponibleException.class);
        assertThatThrownBy(() -> posicion.venderDisponible(new BigDecimal("5"), BigDecimal.ONE, T0))
                .isInstanceOf(VentaNoDisponibleException.class);
    }

    @Test
    void valorarActualizaElPrecioElValorYElPnlNoRealizado() {
        Posicion posicion = Posicion.abrir(USUARIO, "AAPL", T0);
        posicion.comprar(new BigDecimal("10"), new BigDecimal("185.00"), T0);

        posicion.valorar(new BigDecimal("200.00"), T0.plusSeconds(300));

        assertThat(posicion.getPrecioActual()).isEqualByComparingTo("200.0000");
        assertThat(posicion.getValorActual()).isEqualByComparingTo("2000.0000");
        assertThat(posicion.getPnlNoRealizado()).isEqualByComparingTo("150.0000");
        assertThat(posicion.getCostoPromedio()).isEqualByComparingTo("185.0000");
        assertThat(posicion.getCantidad()).isEqualByComparingTo("10");
        assertThat(posicion.getValoradoEn()).isEqualTo(T0.plusSeconds(300));
        assertThat(posicion.getActualizadoEn()).isEqualTo(T0.plusSeconds(300));
    }

    @Test
    void valorarCalculaPerdidaYRedondeaConEscalaCuatro() {
        Posicion posicion = Posicion.abrir(USUARIO, "AAPL", T0);
        posicion.comprar(new BigDecimal("3"), new BigDecimal("10.0000"), T0);

        posicion.valorar(new BigDecimal("9.3333"), T0.plusSeconds(1));

        assertThat(posicion.getValorActual()).isEqualByComparingTo("27.9999");
        assertThat(posicion.getPnlNoRealizado()).isEqualByComparingTo("-2.0001");
    }

    @Test
    void valorarExigeUnPrecioPositivoYElInstante() {
        Posicion posicion = Posicion.abrir(USUARIO, "AAPL", T0);
        posicion.comprar(BigDecimal.TEN, new BigDecimal("100.00"), T0);

        assertThatThrownBy(() -> posicion.valorar(BigDecimal.ZERO, T0))
                .isInstanceOf(PrecioInvalidoException.class);
        assertThatThrownBy(() -> posicion.valorar(null, T0))
                .isInstanceOf(PrecioInvalidoException.class);
        assertThatThrownBy(() -> posicion.valorar(BigDecimal.ONE, null))
                .isInstanceOf(NullPointerException.class);
        assertThat(posicion.getPrecioActual()).isEqualByComparingTo("100.0000");
    }

    @Test
    void validaLasCantidadesYLosInstantesDeReserva() {        Posicion posicion = Posicion.abrir(USUARIO, "AAPL", T0);
        posicion.comprar(BigDecimal.TEN, new BigDecimal("100.00"), T0);

        assertThatThrownBy(() -> posicion.reservar(BigDecimal.ZERO, T0))
                .isInstanceOf(CantidadInvalidaException.class);
        assertThatThrownBy(() -> posicion.reservar(BigDecimal.ONE, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> posicion.liberarReserva(BigDecimal.ZERO, T0))
                .isInstanceOf(CantidadInvalidaException.class);
        assertThatThrownBy(() -> posicion.liberarReserva(BigDecimal.ONE, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void exigeElInstanteDeLaCompra() {
        Posicion posicion = Posicion.abrir(USUARIO, "AAPL", T0);

        assertThatThrownBy(() -> posicion.comprar(BigDecimal.ONE, BigDecimal.ONE, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void exigeElInstanteDeApertura() {
        assertThatThrownBy(() -> Posicion.abrir(USUARIO, "AAPL", null))
                .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "AAPL US", "A@PL", "-AAPL"})
    void rechazaSimbolosInvalidos(String symbol) {
        assertThatThrownBy(() -> Posicion.abrir(USUARIO, symbol, T0))
                .isInstanceOf(SimboloInvalidoException.class);
    }

    @Test
    void rechazaSimbolosDeMasDeVeinteCaracteres() {
        assertThatThrownBy(() -> Posicion.abrir(USUARIO, "A".repeat(21), T0))
                .isInstanceOf(SimboloInvalidoException.class);
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L})
    void rechazaUsuariosNoPositivos(long userId) {
        assertThatThrownBy(() -> Posicion.abrir(userId, "AAPL", T0))
                .isInstanceOf(UsuarioInvalidoException.class);
    }

    @Test
    void rechazaUsuarioNulo() {
        assertThatThrownBy(() -> Posicion.abrir(null, "AAPL", T0))
                .isInstanceOf(UsuarioInvalidoException.class);
    }
}

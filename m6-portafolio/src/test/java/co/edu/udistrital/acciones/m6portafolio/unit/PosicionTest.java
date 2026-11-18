package co.edu.udistrital.acciones.m6portafolio.unit;

import co.edu.udistrital.acciones.m6portafolio.domain.Posicion;
import co.edu.udistrital.acciones.m6portafolio.domain.excepcion.CantidadInvalidaException;
import co.edu.udistrital.acciones.m6portafolio.domain.excepcion.PrecioInvalidoException;
import co.edu.udistrital.acciones.m6portafolio.domain.excepcion.SimboloInvalidoException;
import co.edu.udistrital.acciones.m6portafolio.domain.excepcion.UsuarioInvalidoException;
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

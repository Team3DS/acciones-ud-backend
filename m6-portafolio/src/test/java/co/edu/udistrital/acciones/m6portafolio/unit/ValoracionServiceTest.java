package co.edu.udistrital.acciones.m6portafolio.unit;

import co.edu.udistrital.acciones.m6portafolio.application.dto.PortafolioResponse;
import co.edu.udistrital.acciones.m6portafolio.application.dto.PosicionResponse;
import co.edu.udistrital.acciones.m6portafolio.application.service.ValoracionService;
import co.edu.udistrital.acciones.m6portafolio.domain.Posicion;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ValoracionServiceTest {

    private static final Long USUARIO = 4101L;
    private static final Instant T0 = Instant.parse("2026-01-02T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-02T12:00:00Z"), ZoneOffset.UTC);

    private final ValoracionService servicio = new ValoracionService(CLOCK);

    @Test
    void valoraUnaPosicionConElPrecioGuardado() {
        Posicion posicion = Posicion.abrir(USUARIO, "AAPL", T0);
        posicion.comprar(new BigDecimal("10"), new BigDecimal("180.00"), T0);
        posicion.comprar(new BigDecimal("10"), new BigDecimal("190.00"), T0.plusSeconds(60));

        PosicionResponse respuesta = servicio.valorar(posicion);

        assertThat(respuesta.userId()).isEqualTo(USUARIO);
        assertThat(respuesta.symbol()).isEqualTo("AAPL");
        assertThat(respuesta.quantity()).isEqualByComparingTo("20");
        assertThat(respuesta.reservedQuantity()).isEqualByComparingTo("0");
        assertThat(respuesta.availableQuantity()).isEqualByComparingTo("20");
        assertThat(respuesta.averageCost()).isEqualByComparingTo("185.0000");
        assertThat(respuesta.currentPrice()).isEqualByComparingTo("190.0000");
        assertThat(respuesta.marketValue()).isEqualByComparingTo("3800.0000");
        assertThat(respuesta.unrealizedPnl()).isEqualByComparingTo("100.0000");
        assertThat(respuesta.unrealizedPnlPct()).isEqualByComparingTo("2.7027");
        assertThat(respuesta.realizedPnl()).isEqualByComparingTo("0");
        assertThat(respuesta.priceTimestamp()).isEqualTo(T0.plusSeconds(60));
        assertThat(respuesta.stale()).isFalse();
        assertThat(respuesta.currency()).isEqualTo("USD");
    }

    @Test
    void marcaStaleYDejaLosValoresEnNullCuandoNoHayPrecio() {
        Posicion posicion = Posicion.abrir(USUARIO, "AAPL", T0);

        PosicionResponse respuesta = servicio.valorar(posicion);

        assertThat(respuesta.currentPrice()).isNull();
        assertThat(respuesta.marketValue()).isNull();
        assertThat(respuesta.unrealizedPnl()).isNull();
        assertThat(respuesta.unrealizedPnlPct()).isNull();
        assertThat(respuesta.priceTimestamp()).isNull();
        assertThat(respuesta.stale()).isTrue();
    }

    @Test
    void consolidaElPortafolioConTodasLasPosicionesAbiertas() {
        Posicion aapl = Posicion.abrir(USUARIO, "AAPL", T0);
        aapl.comprar(BigDecimal.TEN, new BigDecimal("180.00"), T0);
        Posicion msft = Posicion.abrir(USUARIO, "MSFT", T0);
        msft.comprar(new BigDecimal("5"), new BigDecimal("100.00"), T0);

        PortafolioResponse respuesta = servicio.valorarPortafolio(USUARIO, List.of(aapl, msft), List.of(aapl, msft));

        assertThat(respuesta.positions()).hasSize(2);
        assertThat(respuesta.totalValue()).isEqualByComparingTo("2300.0000");
        assertThat(respuesta.totalCost()).isEqualByComparingTo("2300.0000");
        assertThat(respuesta.unrealizedPnl()).isEqualByComparingTo("0.0000");
        assertThat(respuesta.realizedPnl()).isEqualByComparingTo("0.0000");
        assertThat(respuesta.currency()).isEqualTo("USD");
        assertThat(respuesta.valuedAt()).isEqualTo(Instant.parse("2026-01-02T12:00:00Z"));
        assertThat(respuesta.stale()).isFalse();
    }

    @Test
    void marcaStaleElPortafolioSiAlgunaPosicionNoTienePrecio() {
        Posicion aapl = Posicion.abrir(USUARIO, "AAPL", T0);
        aapl.comprar(BigDecimal.TEN, new BigDecimal("180.00"), T0);
        Posicion sinPrecio = Posicion.abrir(USUARIO, "MSFT", T0);

        PortafolioResponse respuesta = servicio.valorarPortafolio(USUARIO, List.of(aapl, sinPrecio),
                List.of(aapl, sinPrecio));

        assertThat(respuesta.totalValue()).isNull();
        assertThat(respuesta.unrealizedPnl()).isNull();
        assertThat(respuesta.totalCost()).isEqualByComparingTo("1800.0000");
        assertThat(respuesta.stale()).isTrue();
    }

    @Test
    void devuelveCerosParaUnPortafolioVacio() {
        PortafolioResponse respuesta = servicio.valorarPortafolio(USUARIO, List.of(), List.of());

        assertThat(respuesta.positions()).isEmpty();
        assertThat(respuesta.totalValue()).isEqualByComparingTo("0");
        assertThat(respuesta.totalCost()).isEqualByComparingTo("0");
        assertThat(respuesta.unrealizedPnl()).isEqualByComparingTo("0");
        assertThat(respuesta.realizedPnl()).isEqualByComparingTo("0");
        assertThat(respuesta.stale()).isFalse();
    }
}

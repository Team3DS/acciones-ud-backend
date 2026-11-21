package co.edu.udistrital.acciones.m6portafolio.unit;

import co.edu.udistrital.acciones.m6portafolio.application.dto.CotizacionMercado;
import co.edu.udistrital.acciones.m6portafolio.application.dto.EstadoMercado;
import co.edu.udistrital.acciones.m6portafolio.application.dto.PosicionValoradaPayload;
import co.edu.udistrital.acciones.m6portafolio.application.dto.RoutingKeys;
import co.edu.udistrital.acciones.m6portafolio.application.exception.DatosMercadoNoDisponibleException;
import co.edu.udistrital.acciones.m6portafolio.application.port.out.DatosMercadoClientPort;
import co.edu.udistrital.acciones.m6portafolio.application.port.out.OutboxPort;
import co.edu.udistrital.acciones.m6portafolio.application.port.out.PosicionRepositoryPort;
import co.edu.udistrital.acciones.m6portafolio.application.usecase.ValorarPosicionesUseCase;
import co.edu.udistrital.acciones.m6portafolio.domain.Posicion;
import co.edu.udistrital.acciones.m6portafolio.infrastructure.config.ValoracionProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValorarPosicionesUseCaseTest {

    private static final Instant T0 = Instant.parse("2026-05-02T14:00:00Z");
    private static final Instant VALORADA_EN = Instant.parse("2026-05-02T14:05:00Z");
    private static final Clock CLOCK = Clock.fixed(VALORADA_EN, ZoneOffset.UTC);
    private static final BigDecimal PRECIO = new BigDecimal("200.0000");

    @Mock
    private PosicionRepositoryPort posiciones;

    @Mock
    private DatosMercadoClientPort datosMercado;

    @Mock
    private OutboxPort outbox;

    @Mock
    private PlatformTransactionManager gestor;

    private ValorarPosicionesUseCase caso;

    @BeforeEach
    void prepararCaso() {
        lenient().when(gestor.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        caso = new ValorarPosicionesUseCase(posiciones, datosMercado, outbox,
                new ValoracionProperties(true, 30, true, 200), gestor, CLOCK);
    }

    @Test
    void conElMercadoCerradoNoValoraNada() {
        when(datosMercado.estadoMercado()).thenReturn(new EstadoMercado("CERRADO", T0));

        assertThat(caso.ejecutar()).isZero();

        verifyNoInteractions(posiciones, outbox);
    }

    @Test
    void noValoraCuandoM4NoEstaDisponible() {
        when(datosMercado.estadoMercado())
                .thenThrow(new DatosMercadoNoDisponibleException("M4 no esta disponible", null));

        assertThatCode(() -> assertThat(caso.ejecutar()).isZero()).doesNotThrowAnyException();

        verifyNoInteractions(posiciones, outbox);
    }

    @Test
    void noValoraCuandoFallaLaConsultaDeCotizaciones() {
        when(datosMercado.estadoMercado()).thenReturn(new EstadoMercado("ABIERTO", T0));
        when(posiciones.findSymbolsConCantidad()).thenReturn(List.of("AAPL"));
        when(datosMercado.obtenerCotizaciones(Set.of("AAPL")))
                .thenThrow(new DatosMercadoNoDisponibleException("M4 no esta disponible", null));

        assertThat(caso.ejecutar()).isZero();

        verify(outbox, never()).registrar(any(), any(), any(), any());
    }

    @Test
    void devuelveCeroCuandoNoHayPosicionesAbiertas() {
        when(datosMercado.estadoMercado()).thenReturn(new EstadoMercado("ABIERTO", T0));
        when(posiciones.findSymbolsConCantidad()).thenReturn(List.of());

        assertThat(caso.ejecutar()).isZero();

        verify(datosMercado, never()).obtenerCotizaciones(any());
        verify(outbox, never()).registrar(any(), any(), any(), any());
    }

    @Test
    void valorizaLasPosicionesConPrecioDistintoYEmiteElEvento() {
        Posicion posicion = posicionConId(1L, 4101L, "AAPL", "10", "185.00");
        when(datosMercado.estadoMercado()).thenReturn(new EstadoMercado("ABIERTO", T0));
        when(posiciones.findSymbolsConCantidad()).thenReturn(List.of("AAPL"));
        when(datosMercado.obtenerCotizaciones(Set.of("AAPL")))
                .thenReturn(Map.of("AAPL", new CotizacionMercado("AAPL", PRECIO, T0)));
        when(posiciones.findAbiertasPorSymbolConPrecioDistinto("AAPL", PRECIO, 200))
                .thenReturn(List.of(posicion), List.of());
        when(posiciones.save(any(Posicion.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        assertThat(caso.ejecutar()).isEqualTo(1);

        assertThat(posicion.getPrecioActual()).isEqualByComparingTo("200.0000");
        assertThat(posicion.getValorActual()).isEqualByComparingTo("2000.0000");
        assertThat(posicion.getPnlNoRealizado()).isEqualByComparingTo("150.0000");
        assertThat(posicion.getValoradoEn()).isEqualTo(VALORADA_EN);
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(outbox).registrar(eq(RoutingKeys.TIPO_AGREGADO_POSICION), eq("1"),
                eq(RoutingKeys.POSICION_VALORADA), captor.capture());
        PosicionValoradaPayload evento = (PosicionValoradaPayload) captor.getValue();
        assertThat(evento.userId()).isEqualTo(4101L);
        assertThat(evento.symbol()).isEqualTo("AAPL");
        assertThat(evento.quantity()).isEqualByComparingTo("10");
        assertThat(evento.price()).isEqualByComparingTo("200.0000");
        assertThat(evento.marketValue()).isEqualByComparingTo("2000.0000");
        assertThat(evento.unrealizedPnl()).isEqualByComparingTo("150.0000");
        assertThat(evento.valuedAt()).isEqualTo(VALORADA_EN);
    }

    @Test
    void procesaLotesSucesivosHastaAgotarLasPosiciones() {
        ValorarPosicionesUseCase porLotes = new ValorarPosicionesUseCase(posiciones, datosMercado, outbox,
                new ValoracionProperties(true, 30, true, 1), gestor, CLOCK);
        Posicion primera = posicionConId(1L, 4101L, "AAPL", "10", "185.00");
        Posicion segunda = posicionConId(2L, 4102L, "AAPL", "5", "185.00");
        when(datosMercado.estadoMercado()).thenReturn(new EstadoMercado("ABIERTO", T0));
        when(posiciones.findSymbolsConCantidad()).thenReturn(List.of("AAPL"));
        when(datosMercado.obtenerCotizaciones(Set.of("AAPL")))
                .thenReturn(Map.of("AAPL", new CotizacionMercado("AAPL", PRECIO, T0)));
        when(posiciones.findAbiertasPorSymbolConPrecioDistinto("AAPL", PRECIO, 1))
                .thenReturn(List.of(primera), List.of(segunda), List.of());
        when(posiciones.save(any(Posicion.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        assertThat(porLotes.ejecutar()).isEqualTo(2);

        verify(posiciones, times(3))
                .findAbiertasPorSymbolConPrecioDistinto("AAPL", PRECIO, 1);
    }

    @Test
    void unErrorEnUnSimboloNoDetieneALosDemas() {
        Posicion posicion = posicionConId(2L, 4102L, "MSFT", "5", "185.00");
        Map<String, CotizacionMercado> cotizaciones = new LinkedHashMap<>();
        cotizaciones.put("AAPL", new CotizacionMercado("AAPL", PRECIO, T0));
        cotizaciones.put("MSFT", new CotizacionMercado("MSFT", PRECIO, T0));
        when(datosMercado.estadoMercado()).thenReturn(new EstadoMercado("ABIERTO", T0));
        when(posiciones.findSymbolsConCantidad()).thenReturn(List.of("AAPL", "MSFT"));
        when(datosMercado.obtenerCotizaciones(Set.of("AAPL", "MSFT"))).thenReturn(cotizaciones);
        when(posiciones.findAbiertasPorSymbolConPrecioDistinto(eq("AAPL"), eq(PRECIO), anyInt()))
                .thenThrow(new IllegalStateException("fallo de base de datos"));
        when(posiciones.findAbiertasPorSymbolConPrecioDistinto(eq("MSFT"), eq(PRECIO), anyInt()))
                .thenReturn(List.of(posicion), List.of());
        when(posiciones.save(any(Posicion.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        assertThat(caso.ejecutar()).isEqualTo(1);

        verify(outbox).registrar(anyString(), eq("2"), eq(RoutingKeys.POSICION_VALORADA), any());
    }

    @Test
    void valoraSinConsultarElEstadoCuandoElMercadoNoEsObligatorio() {
        ValorarPosicionesUseCase sinRestriccion = new ValorarPosicionesUseCase(posiciones, datosMercado, outbox,
                new ValoracionProperties(true, 30, false, 200), gestor, CLOCK);
        Posicion posicion = posicionConId(1L, 4101L, "AAPL", "10", "185.00");
        when(posiciones.findSymbolsConCantidad()).thenReturn(List.of("AAPL"));
        when(datosMercado.obtenerCotizaciones(Set.of("AAPL")))
                .thenReturn(Map.of("AAPL", new CotizacionMercado("AAPL", PRECIO, T0)));
        when(posiciones.findAbiertasPorSymbolConPrecioDistinto("AAPL", PRECIO, 200))
                .thenReturn(List.of(posicion), List.of());
        when(posiciones.save(any(Posicion.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        assertThat(sinRestriccion.ejecutar()).isEqualTo(1);

        verify(datosMercado, never()).estadoMercado();
    }

    private static Posicion posicionConId(Long id, Long userId, String symbol, String cantidad,
                                          String costoPromedio) {
        Posicion posicion = Posicion.abrir(userId, symbol, T0);
        posicion.comprar(new BigDecimal(cantidad), new BigDecimal(costoPromedio), T0);
        ReflectionTestUtils.setField(posicion, "id", id);
        return posicion;
    }
}

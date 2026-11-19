package co.edu.udistrital.acciones.m3catalogomercados.unit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import co.edu.udistrital.acciones.m3catalogomercados.application.port.out.CatalogoRepositoryPort;
import co.edu.udistrital.acciones.m3catalogomercados.application.service.CatalogoService;
import co.edu.udistrital.acciones.m3catalogomercados.domain.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

@ExtendWith(MockitoExtension.class)
class CatalogoServiceTest {
    @Mock CatalogoRepositoryPort repository;
    @InjectMocks CatalogoService service;
    private final Mercado nasdaq =
            new Mercado((short) 1, "NASDAQ", "Nasdaq", "XNAS", "America/New_York", "USD", true);
    private final Sector tecnologia = new Sector((short) 1, "TECNOLOGIA", "Tecnología", "Software");

    private Instrumento accion(boolean enabled, Mercado market) {
        return new Instrumento(1L, "AAPL", "Apple Inc.", enabled, market, tecnologia);
    }

    @Test
    void validaNormalizaYDeduplicaEnUnaConsulta() {
        when(repository.buscarPorSimbolos(List.of("AAPL", "ZZZZ"), "NASDAQ"))
                .thenReturn(List.of(accion(true, nasdaq)));
        var result = service.validar(List.of(" aapl ", "AAPL", "zzzz"), "nasdaq");
        assertThat(result)
                .extracting(ValidacionInstrumento::symbol)
                .containsExactly("AAPL", "ZZZZ");
        assertThat(result.getFirst().valid()).isTrue();
        assertThat(result.getFirst().reason()).isEqualTo(MotivoElegibilidad.HABILITADO);
        assertThat(result.get(1).reason()).isEqualTo(MotivoElegibilidad.NO_ENCONTRADO);
        verify(repository).buscarPorSimbolos(List.of("AAPL", "ZZZZ"), "NASDAQ");
    }

    @Test
    void rechazaAccionDeshabilitada() {
        when(repository.buscarPorSimbolos(any(), isNull()))
                .thenReturn(List.of(accion(false, nasdaq)));
        var result = service.validar(List.of("AAPL"), null).getFirst();
        assertThat(result.valid()).isFalse();
        assertThat(result.reason()).isEqualTo(MotivoElegibilidad.ACCION_DESHABILITADA);
    }

    @Test
    void mercadoDeshabilitadoImpideOperarAunqueLaAccionEsteHabilitada() {
        var market =
                new Mercado(
                        (short) 1, "NASDAQ", "Nasdaq", "XNAS", "America/New_York", "USD", false);
        when(repository.buscarPorSimbolos(any(), isNull()))
                .thenReturn(List.of(accion(true, market)));
        var result = service.validar(List.of("AAPL"), null).getFirst();
        assertThat(result.valid()).isFalse();
        assertThat(result.reason()).isEqualTo(MotivoElegibilidad.MERCADO_DESHABILITADO);
    }

    @Test
    void noEscogeUnMercadoSilenciosamenteSiElTickerEsAmbiguo() {
        when(repository.buscarPorSimbolos(any(), isNull()))
                .thenReturn(List.of(accion(true, nasdaq), accion(true, nasdaq)));
        assertThat(service.validar(List.of("AAPL"), null).getFirst().reason())
                .isEqualTo(MotivoElegibilidad.SIMBOLO_AMBIGUO);
        assertThatThrownBy(() -> service.instrumento("AAPL", null))
                .isInstanceOfSatisfying(
                        CatalogoException.class,
                        ex -> {
                            assertThat(ex.status()).isEqualTo(409);
                            assertThat(ex.code()).isEqualTo("SIMBOLO_AMBIGUO");
                        });
    }

    @Test
    void detalleDevuelveMetadatosAunqueEsteDeshabilitada() {
        when(repository.buscarPorSimbolos(List.of("AAPL"), "NASDAQ"))
                .thenReturn(List.of(accion(false, nasdaq)));
        assertThat(service.instrumento("aapl", "nasdaq").enabled()).isFalse();
    }

    @Test
    void detalleDesconocidoEs404() {
        when(repository.buscarPorSimbolos(any(), any())).thenReturn(List.of());
        assertThatThrownBy(() -> service.instrumento("ZZZZ", null))
                .isInstanceOfSatisfying(
                        CatalogoException.class, ex -> assertThat(ex.status()).isEqualTo(404));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "SYMBOL_TOO_LONG_FOR_M3_LIMIT", "AAPL;DROP", "A/B", "%"})
    void rechazaSimbolosInvalidosAntesDeConsultar(String symbol) {
        assertThatThrownBy(() -> service.validar(Collections.singletonList(symbol), null))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void limitaElLote() {
        assertThatThrownBy(() -> service.validar(null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.validar(List.of(), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.validar(Collections.nCopies(101, "AAPL"), null))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void normalizaLosFiltrosSinMezclarDatosDeM4() {
        when(repository.buscar(any()))
                .thenReturn(Pagina.of(List.of(accion(true, nasdaq)), 0, 10, 1));
        var result =
                service.buscar(
                        " Apple ", "nasdaq", "tecnologia", true, 0, 10, "company_name", "DESC");
        ArgumentCaptor<FiltroInstrumentos> captor =
                ArgumentCaptor.forClass(FiltroInstrumentos.class);
        verify(repository).buscar(captor.capture());
        assertThat(captor.getValue())
                .isEqualTo(
                        new FiltroInstrumentos(
                                "Apple",
                                "NASDAQ",
                                "TECNOLOGIA",
                                true,
                                0,
                                10,
                                OrdenCatalogo.COMPANY_NAME,
                                true));
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @Test
    void permiteBusquedaSinFiltros() {
        service.buscar(null, null, null, null, 0, 20, "symbol", "asc");
        verify(repository)
                .buscar(
                        new FiltroInstrumentos(
                                null, null, null, null, 0, 20, OrdenCatalogo.SYMBOL, false));
    }

    @Test
    void validaLimitesYOrdenamiento() {
        assertThatThrownBy(() -> service.buscar(null, null, null, true, -1, 20, "SYMBOL", "asc"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.buscar(null, null, null, true, 0, 0, "SYMBOL", "asc"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.buscar(null, null, null, true, 0, 101, "SYMBOL", "asc"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () ->
                                service.buscar(
                                        "x".repeat(151), null, null, true, 0, 20, "SYMBOL", "asc"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.buscar(null, null, null, true, 0, 20, "PRICE", "asc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("M4");
        assertThatThrownBy(() -> service.buscar(null, null, null, true, 0, 20, "SYMBOL", "bad"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.buscar(null, "bad!", null, true, 0, 20, "SYMBOL", "asc"))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void buscaMercadosConCodigoNormalizado() {
        when(repository.buscarMercado("NASDAQ")).thenReturn(Optional.of(nasdaq));
        assertThat(service.mercado(" nasdaq ")).isEqualTo(nasdaq);
        when(repository.buscarMercado("UNKNOWN")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.mercado("UNKNOWN"))
                .isInstanceOfSatisfying(
                        CatalogoException.class, ex -> assertThat(ex.status()).isEqualTo(404));
    }

    @Test
    void conservaListadosYDetallePorIdentificador() {
        when(repository.listarMercados(true)).thenReturn(List.of(nasdaq));
        when(repository.listarSectores()).thenReturn(List.of(tecnologia));
        when(repository.buscarPorId(1L)).thenReturn(Optional.of(accion(true, nasdaq)));
        assertThat(service.mercados(true)).containsExactly(nasdaq);
        assertThat(service.sectores()).containsExactly(tecnologia);
        assertThat(service.instrumento(1).id()).isEqualTo(1);
        assertThatThrownBy(() -> service.instrumento(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.instrumento(2))
                .isInstanceOfSatisfying(
                        CatalogoException.class, ex -> assertThat(ex.status()).isEqualTo(404));
    }
}

package co.edu.udistrital.acciones.m3catalogomercados.unit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import co.edu.udistrital.acciones.m3catalogomercados.application.command.*;
import co.edu.udistrital.acciones.m3catalogomercados.application.port.out.CatalogoAdminPort;
import co.edu.udistrital.acciones.m3catalogomercados.application.service.CatalogoAdminService;
import co.edu.udistrital.acciones.m3catalogomercados.domain.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.function.Supplier;

@ExtendWith(MockitoExtension.class)
class CatalogoAdminServiceTest {
    @Mock CatalogoAdminPort port;
    CatalogoAdminService service;
    final Mercado market =
            new Mercado((short) 1, "NASDAQ", "Nasdaq", "XNAS", "America/New_York", "USD", true);
    final Instrumento instrument =
            new Instrumento(
                    1,
                    "AAPL",
                    "Apple",
                    true,
                    market,
                    new Sector((short) 1, "TECNOLOGIA", "Tecnología", null));

    @BeforeEach
    void setup() {
        service = new CatalogoAdminService(port);
        lenient()
                .doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(5)).get())
                .when(port)
                .ejecutar(anyString(), anyString(), anyLong(), any(), any(), any());
    }

    @Test
    void creaMercadoYProgramaEvento() {
        var command =
                new CrearMercadoCommand(
                        "NASDAQ", "Nasdaq", "XNAS", "America/New_York", "USD", null, null);
        when(port.crearMercado(command)).thenReturn(market);
        assertThat(service.crearMercado(command, "key", 1).status()).isEqualTo(201);
        verify(port)
                .registrarEvento(
                        eq("m3.mercado.creado"),
                        eq("NASDAQ"),
                        argThat(value -> value.toString().contains("timezone=America/New_York")));
    }

    @Test
    void validaZonaYHorarioDeReferencia() {
        assertThatThrownBy(
                        () ->
                                service.crearMercado(
                                        new CrearMercadoCommand(
                                                "X",
                                                "Test",
                                                "XTST",
                                                "not/a-zone",
                                                "USD",
                                                null,
                                                null),
                                        "key",
                                        1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () ->
                                service.crearMercado(
                                        new CrearMercadoCommand(
                                                "X",
                                                "Test",
                                                "XTST",
                                                "UTC",
                                                "USD",
                                                null,
                                                new HorarioReferencia(LocalTime.NOON, null)),
                                        "key",
                                        1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void creaInstrumentoYEventoConDatosDeIntegracion() {
        var command =
                new CrearInstrumentoCommand(
                        "AAPL",
                        "Apple",
                        "NASDAQ",
                        "TECNOLOGIA",
                        TipoInstrumento.ACCION,
                        BigDecimal.ONE,
                        new BigDecimal("0.01"));
        when(port.crearInstrumento(command)).thenReturn(instrument);
        assertThat(service.crearInstrumento(command, "key", 1).body()).isEqualTo(instrument);
        verify(port)
                .registrarEvento(
                        eq("m3.instrumento.creado"),
                        eq("1"),
                        argThat(value -> value.toString().contains("currency=USD")));
    }

    @Test
    void modificaMetadatosDelMercado() {
        var command =
                new ActualizarMercadoCommand(
                        "Nasdaq",
                        false,
                        null,
                        new HorarioReferencia(LocalTime.NOON, LocalTime.MIDNIGHT));
        when(port.actualizarMercado("NASDAQ", command)).thenReturn(market);
        assertThat(service.actualizarMercado("NASDAQ", command, "key", 1).status()).isEqualTo(200);
        verify(port)
                .registrarEvento(
                        eq("m3.mercado.actualizado"),
                        eq("NASDAQ"),
                        argThat(value -> value.toString().contains("mic=XNAS")));
    }

    @Test
    void rechazaActualizacionDeMercadoVaciaONombreEnBlanco() {
        assertThatThrownBy(
                        () ->
                                service.actualizarMercado(
                                        "NASDAQ",
                                        new ActualizarMercadoCommand(null, null, null, null),
                                        "key",
                                        1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () ->
                                service.actualizarMercado(
                                        "NASDAQ",
                                        new ActualizarMercadoCommand(" ", null, null, null),
                                        "key",
                                        1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void modificaInstrumentoConOSinMercadoExplicito() {
        var command =
                new ActualizarInstrumentoCommand(null, null, null, EstadoInstrumento.SUSPENDIDO);
        when(port.actualizarInstrumento("AAPL", null, command)).thenReturn(instrument);
        when(port.actualizarInstrumento("AAPL", "NASDAQ", command)).thenReturn(instrument);
        assertThat(service.actualizarInstrumento("AAPL", null, command, "key", 1).status())
                .isEqualTo(200);
        assertThat(service.actualizarInstrumento("AAPL", "NASDAQ", command, "key", 1).status())
                .isEqualTo(200);
    }

    @Test
    void rechazaActualizacionDeInstrumentoVaciaONombreEnBlanco() {
        assertThatThrownBy(
                        () ->
                                service.actualizarInstrumento(
                                        "AAPL",
                                        null,
                                        new ActualizarInstrumentoCommand(null, null, null, null),
                                        "key",
                                        1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () ->
                                service.actualizarInstrumento(
                                        "AAPL",
                                        null,
                                        new ActualizarInstrumentoCommand(" ", null, null, null),
                                        "key",
                                        1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

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

import static org.junit.jupiter.api.Assertions.fail;

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
                .doAnswer(invocation -> {
                    Supplier<?> supplier = invocation.getArgument(5);
                    return supplier != null ? supplier.get() : null;
                })
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

    @Test
    void idempotenciaConflictoCuandoHashDifiere() {
        when(port.ejecutar(anyString(), anyString(), anyLong(), any(), any(), any()))
                .thenThrow(new co.edu.udistrital.acciones.m3catalogomercados.domain.CatalogoException(
                        "IDEMPOTENCIA_CONFLICTO", 409, "La clave ya se usó con otro contenido."));
        var command = new CrearMercadoCommand("TEST", "Test", "XTST", "UTC", "USD", null, null);
        try {
            service.crearMercado(command, "key", 1);
            fail("Expected CatalogoException");
        } catch (co.edu.udistrital.acciones.m3catalogomercados.domain.CatalogoException ex) {
            assertThat(ex.code()).isEqualTo("IDEMPOTENCIA_CONFLICTO");
        }
    }

    @Test
    void comandoPendienteCuandoEjecucionEnProceso() {
        when(port.ejecutar(anyString(), anyString(), anyLong(), any(), any(), any()))
                .thenThrow(new co.edu.udistrital.acciones.m3catalogomercados.domain.CatalogoException(
                        "COMANDO_PENDIENTE", 409, "El comando todavía está en proceso."));
        var command = new CrearMercadoCommand("TEST", "Test", "XTST", "UTC", "USD", null, null);
        try {
            service.crearMercado(command, "key", 1);
            fail("Expected CatalogoException");
        } catch (co.edu.udistrital.acciones.m3catalogomercados.domain.CatalogoException ex) {
            assertThat(ex.code()).isEqualTo("COMANDO_PENDIENTE");
        }
    }

    @Test
    void validaKeyIdempotenciaFormatoInvalido() {
        when(port.ejecutar(anyString(), anyString(), anyLong(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException(
                        "Idempotency-Key requiere de 1 a 100 letras, números, puntos, guiones o guiones bajos."));
        var command = new CrearMercadoCommand("TEST", "Test", "XTST", "UTC", "USD", null, null);
        assertThatThrownBy(() -> service.crearMercado(command, "invalid key!", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Idempotency-Key");
    }

    @Test
    void actualizaMercadoConSoloPais() {
        var command = new ActualizarMercadoCommand(null, null, "CO", null);
        when(port.actualizarMercado("NASDAQ", command)).thenReturn(market);
        assertThat(service.actualizarMercado("NASDAQ", command, "key", 1).status()).isEqualTo(200);
        verify(port).registrarEvento(eq("m3.mercado.actualizado"), eq("NASDAQ"), any());
    }

    @Test
    void actualizaInstrumentoCambiaEstadoYSuspende() {
        var command = new ActualizarInstrumentoCommand(null, null, null, EstadoInstrumento.SUSPENDIDO);
        when(port.actualizarInstrumento("AAPL", "NASDAQ", command)).thenReturn(instrument);
        assertThat(service.actualizarInstrumento("AAPL", "NASDAQ", command, "key", 1).status()).isEqualTo(200);
        verify(port).registrarEvento(eq("m3.instrumento.actualizado"), eq("1"), any());
    }
}

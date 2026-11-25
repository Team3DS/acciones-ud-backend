package co.edu.udistrital.acciones.m5ordenesejecucion.application.service;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.ProcesarEjecucionUseCase;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.ConfiguracionComisionRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.ConfirmacionEjecucionRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.EjecucionOrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.FondosClientPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.HistorialEstadoOrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.OrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.OutboxEventoRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.PortafolioClientPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.PublicadorOrdenesPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.ConfiguracionComision;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EjecucionOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EstadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.HistorialEstadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.LadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.Orden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.OutboxEvento;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.ServicioParticipante;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;

@ExtendWith(MockitoExtension.class)
class ProcesarEjecucionUseCaseImplTest {

    @Mock
    OrdenRepositoryPort ordenRepository;

    @Mock
    ConfiguracionComisionRepositoryPort comisionRepository;

    @Mock
    EjecucionOrdenRepositoryPort ejecucionRepository;

    @Mock
    ConfirmacionEjecucionRepositoryPort confirmacionRepository;

    @Mock
    HistorialEstadoOrdenRepositoryPort historialRepository;

    @Mock
    OutboxEventoRepositoryPort outboxRepository;

    @Mock
    FondosClientPort fondosClient;

    @Mock
    PortafolioClientPort portafolioClient;

    @Mock
    PublicadorOrdenesPort publicador;

    @Mock
    com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    private ProcesarEjecucionUseCaseImpl service;

    @BeforeEach
    void setUp() {
        service = new ProcesarEjecucionUseCaseImpl(
                ordenRepository, comisionRepository, ejecucionRepository, confirmacionRepository,
                historialRepository, outboxRepository, fondosClient, portafolioClient, publicador, objectMapper
        );
    }

    @Test
    void ejecucionIdempotenteRetornaExistente() {
        EjecucionOrden existente = new EjecucionOrden();
        existente.setId(1L);
        existente.setBrokerExecutionId("exec-123");

        when(ejecucionRepository.findByBrokerExecutionId("exec-123")).thenReturn(Optional.of(existente));

        ProcesarEjecucionUseCase.ProcesarEjecucionCommand cmd = new ProcesarEjecucionUseCase.ProcesarEjecucionCommand(
                null, "exec-123", null, null, null, null, null
        );

        EjecucionOrden resultado = service.ejecutar(cmd);

        assertEquals(1L, resultado.getId());
        assertEquals("exec-123", resultado.getBrokerExecutionId());
        verifyNoInteractions(ordenRepository);
    }

    @Test
    void ordenNoEncontradaLanzaExcepcion() {
        when(ejecucionRepository.findByBrokerExecutionId("exec-123")).thenReturn(Optional.empty());
        when(ordenRepository.findById(1L)).thenReturn(Optional.empty());
        when(ordenRepository.findByBrokerOrderId("broker-456")).thenReturn(Optional.empty());

        ProcesarEjecucionUseCase.ProcesarEjecucionCommand cmd = new ProcesarEjecucionUseCase.ProcesarEjecucionCommand(
                1L, "exec-123", "broker-456", 10, new BigDecimal("150.00"), null, null
        );

        assertThrows(IllegalArgumentException.class, () -> service.ejecutar(cmd));
    }

    @Test
    void ejecutarCompraConComisionNulaCalculaComision() throws Exception {
        Orden orden = new Orden();
        orden.setId(10L);
        orden.setUsuarioId(1L);
        orden.setTicker("AAPL");
        orden.setLadoOrden(LadoOrden.COMPRA);
        orden.setCantidad(10);
        orden.setConfiguracionComisionId(5L);
        orden.setReservaFondosId(100L);

        EjecucionOrden ejecucionGuardada = new EjecucionOrden();
        ejecucionGuardada.setId(1L);
        ejecucionGuardada.setOrdenId(10L);
        ejecucionGuardada.setCantidad(10);

        when(ejecucionRepository.findByBrokerExecutionId("exec-123")).thenReturn(Optional.empty());
        when(ordenRepository.findById(10L)).thenReturn(Optional.of(orden));
        ConfiguracionComision comisionCfg = new ConfiguracionComision();
        comisionCfg.setId(5L);
        comisionCfg.setPorcentaje(new BigDecimal("0.02"));
        when(comisionRepository.findById(5L)).thenReturn(Optional.of(comisionCfg));
        when(ejecucionRepository.save(any())).thenReturn(ejecucionGuardada);
        when(ejecucionRepository.findByOrdenId(10L)).thenReturn(List.of(ejecucionGuardada));
        doNothing().when(fondosClient).confirmarReserva(eq(100L), eq(1L), eq("exec-123"), eq(new BigDecimal("1530.00")), eq(true), eq(1));
        when(portafolioClient.actualizarPosicion(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new BigDecimal("152.00"));

        ProcesarEjecucionUseCase.ProcesarEjecucionCommand cmd = new ProcesarEjecucionUseCase.ProcesarEjecucionCommand(
                10L, "exec-123", null, 10, new BigDecimal("150.00"), null, null
        );

        EjecucionOrden resultado = service.ejecutar(cmd);

        assertEquals(1L, resultado.getId());
        assertEquals(EstadoOrden.EJECUTADA, orden.getEstadoOrden());
        verify(fondosClient).confirmarReserva(eq(100L), eq(1L), eq("exec-123"), eq(new BigDecimal("1530.00")), eq(true), eq(1));
        verify(portafolioClient).actualizarPosicion(
                eq("10"), eq("exec-123"), eq(1L), eq("AAPL"), eq(LadoOrden.COMPRA), eq(10), eq(new BigDecimal("150.00")), any(), any()
        );
        verify(publicador).publicarOrdenEjecutada(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyBoolean(), any());
    }

    @Test
    void ejecutarVentaSinReservaFondos() throws Exception {
        Orden orden = new Orden();
        orden.setId(11L);
        orden.setUsuarioId(2L);
        orden.setTicker("MSFT");
        orden.setLadoOrden(LadoOrden.VENTA);
        orden.setCantidad(5);
        orden.setReservaFondosId(null);

        EjecucionOrden ejecucionGuardada = new EjecucionOrden();
        ejecucionGuardada.setId(2L);
        ejecucionGuardada.setOrdenId(11L);
        ejecucionGuardada.setCantidad(5);

        when(ejecucionRepository.findByBrokerExecutionId("exec-456")).thenReturn(Optional.empty());
        when(ordenRepository.findById(11L)).thenReturn(Optional.of(orden));
        when(ejecucionRepository.save(any())).thenReturn(ejecucionGuardada);
        when(ejecucionRepository.findByOrdenId(11L)).thenReturn(List.of(ejecucionGuardada));
        when(portafolioClient.actualizarPosicion(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new BigDecimal("200.00"));

        ProcesarEjecucionUseCase.ProcesarEjecucionCommand cmd = new ProcesarEjecucionUseCase.ProcesarEjecucionCommand(
                11L, "exec-456", null, 5, new BigDecimal("200.00"), new BigDecimal("1.00"), null
        );

        EjecucionOrden resultado = service.ejecutar(cmd);

        assertEquals(EstadoOrden.EJECUTADA, orden.getEstadoOrden());
        verifyNoInteractions(fondosClient);
        verify(portafolioClient).actualizarPosicion(
                eq("11"), eq("exec-456"), eq(2L), eq("MSFT"), eq(LadoOrden.VENTA), eq(5), eq(new BigDecimal("200.00")), any(), any()
        );
        verify(publicador).publicarOrdenEjecutada(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyBoolean(), any());
    }

    @Test
    void ejecucionParcialmenteEjecutada() throws Exception {
        Orden orden = new Orden();
        orden.setId(12L);
        orden.setUsuarioId(3L);
        orden.setTicker("GOOG");
        orden.setLadoOrden(LadoOrden.COMPRA);
        orden.setCantidad(100);
        orden.setConfiguracionComisionId(5L);
        orden.setReservaFondosId(null);

        EjecucionOrden ejecucionGuardada = new EjecucionOrden();
        ejecucionGuardada.setId(3L);
        ejecucionGuardada.setOrdenId(12L);
        ejecucionGuardada.setCantidad(30);

        when(ejecucionRepository.findByBrokerExecutionId("exec-789")).thenReturn(Optional.empty());
        when(ordenRepository.findById(12L)).thenReturn(Optional.of(orden));
        ConfiguracionComision comisionCfg = new ConfiguracionComision();
        comisionCfg.setId(5L);
        comisionCfg.setPorcentaje(new BigDecimal("0.01"));
        when(comisionRepository.findById(5L)).thenReturn(Optional.of(comisionCfg));
        when(ejecucionRepository.save(any())).thenReturn(ejecucionGuardada);
        when(ejecucionRepository.findByOrdenId(12L)).thenReturn(List.of(ejecucionGuardada));
        when(portafolioClient.actualizarPosicion(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new BigDecimal("2800.00"));

        ProcesarEjecucionUseCase.ProcesarEjecucionCommand cmd = new ProcesarEjecucionUseCase.ProcesarEjecucionCommand(
                12L, "exec-789", null, 30, new BigDecimal("2800.00"), null, null
        );

        service.ejecutar(cmd);

        assertEquals(EstadoOrden.PARCIALMENTE_EJECUTADA, orden.getEstadoOrden());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @SuppressWarnings("unchecked")
    void informaElMismoCierreDeReservaPorRestEventoYOutbox(boolean finalExecution) throws Exception {
        Orden orden = new Orden();
        orden.setId(12L);
        orden.setUsuarioId(42L);
        orden.setTicker("AAPL");
        orden.setLadoOrden(LadoOrden.COMPRA);
        orden.setCantidad(10);
        orden.setReservaFondosId(7L);

        EjecucionOrden actual = new EjecucionOrden();
        actual.setId(2L);
        actual.setOrdenId(12L);
        actual.setCantidad(4);
        actual.setPrecioEjecucion(new BigDecimal("100.00"));
        actual.setComision(BigDecimal.ZERO);
        EjecucionOrden previa = new EjecucionOrden();
        previa.setCantidad(6);

        when(ejecucionRepository.findByBrokerExecutionId("fill-2")).thenReturn(Optional.empty());
        when(ordenRepository.findById(12L)).thenReturn(Optional.of(orden));
        when(ejecucionRepository.save(any())).thenReturn(actual);
        when(ejecucionRepository.findByOrdenId(12L)).thenReturn(finalExecution ? List.of(previa, actual) : List.of(actual));
        when(portafolioClient.actualizarPosicion(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new BigDecimal("100.00"));
        when(objectMapper.writeValueAsString(any(Map.class))).thenReturn("{}");

        service.ejecutar(new ProcesarEjecucionUseCase.ProcesarEjecucionCommand(
                12L, "fill-2", null, 4, new BigDecimal("100.00"), BigDecimal.ZERO, null));

        assertEquals(finalExecution ? EstadoOrden.EJECUTADA : EstadoOrden.PARCIALMENTE_EJECUTADA, orden.getEstadoOrden());
        verify(fondosClient).confirmarReserva(7L, 42L, "fill-2", new BigDecimal("400.00"), finalExecution, finalExecution ? 2 : null);
        verify(publicador).publicarOrdenEjecutada(any(), eq("fill-2"), eq(12L), eq(42L), eq("AAPL"),
                eq("COMPRA"), eq(4), eq(new BigDecimal("100.00")), eq(BigDecimal.ZERO), any(), eq(finalExecution), eq(finalExecution ? 2 : null));
        ArgumentCaptor<Map> sobre = ArgumentCaptor.forClass(Map.class);
        verify(objectMapper).writeValueAsString(sobre.capture());
        Map<String, Object> payload = (Map<String, Object>) sobre.getValue().get("payload");
        assertEquals(finalExecution, payload.get("finalExecution"));
        assertEquals(finalExecution ? 2 : null, payload.get("expectedExecutionCount"));
        assertEquals("fill-2", payload.get("executionId"));
    }

    @Test
    void registrarOutboxGuardaEvento() throws Exception {
        Orden orden = new Orden();
        orden.setId(13L);
        orden.setUsuarioId(4L);
        orden.setTicker("TSLA");
        orden.setLadoOrden(LadoOrden.VENTA);
        orden.setCantidad(2);
        orden.setReservaFondosId(null);

        EjecucionOrden ejecucionGuardada = new EjecucionOrden();
        ejecucionGuardada.setId(4L);
        ejecucionGuardada.setOrdenId(13L);
        ejecucionGuardada.setCantidad(2);
        ejecucionGuardada.setPrecioEjecucion(new BigDecimal("250.00"));
        ejecucionGuardada.setComision(new BigDecimal("0.50"));
        ejecucionGuardada.setEjecutadaEn(Instant.now());

        when(ejecucionRepository.findByBrokerExecutionId("exec-999")).thenReturn(Optional.empty());
        when(ordenRepository.findById(13L)).thenReturn(Optional.of(orden));
        when(ejecucionRepository.save(any())).thenReturn(ejecucionGuardada);
        when(ejecucionRepository.findByOrdenId(13L)).thenReturn(List.of(ejecucionGuardada));
        when(portafolioClient.actualizarPosicion(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new BigDecimal("250.00"));
        when(objectMapper.writeValueAsString(any(Map.class))).thenReturn("{}");

        ProcesarEjecucionUseCase.ProcesarEjecucionCommand cmd = new ProcesarEjecucionUseCase.ProcesarEjecucionCommand(
                13L, "exec-999", null, 2, new BigDecimal("250.00"), null, null
        );

        service.ejecutar(cmd);

        ArgumentCaptor<OutboxEvento> captor = ArgumentCaptor.forClass(OutboxEvento.class);
        verify(outboxRepository).save(captor.capture());
        assertEquals("OrdenEjecutada", captor.getValue().getTipoEvento());
    }

    @Test
    void ordenConComisionSeteadaNoConsultaRepositorio() throws Exception {
        Orden orden = new Orden();
        orden.setId(14L);
        orden.setUsuarioId(5L);
        orden.setTicker("NVDA");
        orden.setLadoOrden(LadoOrden.COMPRA);
        orden.setCantidad(10);
        orden.setConfiguracionComisionId(5L);
        orden.setReservaFondosId(null);

        EjecucionOrden ejecucionGuardada = new EjecucionOrden();
        ejecucionGuardada.setId(5L);
        ejecucionGuardada.setOrdenId(14L);
        ejecucionGuardada.setCantidad(10);

        when(ejecucionRepository.findByBrokerExecutionId("exec-111")).thenReturn(Optional.empty());
        when(ordenRepository.findById(14L)).thenReturn(Optional.of(orden));
        when(ejecucionRepository.save(any())).thenReturn(ejecucionGuardada);
        when(ejecucionRepository.findByOrdenId(14L)).thenReturn(List.of(ejecucionGuardada));
        when(portafolioClient.actualizarPosicion(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new BigDecimal("500.00"));

        ProcesarEjecucionUseCase.ProcesarEjecucionCommand cmd = new ProcesarEjecucionUseCase.ProcesarEjecucionCommand(
                14L, "exec-111", null, 10, new BigDecimal("50.00"), new BigDecimal("10.00"), null
        );

        service.ejecutar(cmd);

        verifyNoInteractions(comisionRepository);
        verify(portafolioClient).actualizarPosicion(
                eq("14"), eq("exec-111"), eq(5L), eq("NVDA"), eq(LadoOrden.COMPRA), eq(10), eq(new BigDecimal("50.00")), any(), any()
        );
    }

    @Test
    void buscarOrdenPorBrokerOrderIdCuandoOrdenIdEsNull() throws Exception {
        Orden orden = new Orden();
        orden.setId(15L);
        orden.setUsuarioId(6L);
        orden.setTicker("AMD");
        orden.setLadoOrden(LadoOrden.VENTA);
        orden.setCantidad(5);
        orden.setReservaFondosId(null);

        EjecucionOrden ejecucionGuardada = new EjecucionOrden();
        ejecucionGuardada.setId(6L);
        ejecucionGuardada.setOrdenId(15L);
        ejecucionGuardada.setCantidad(5);

        when(ejecucionRepository.findByBrokerExecutionId("exec-222")).thenReturn(Optional.empty());
        when(ordenRepository.findByBrokerOrderId("broker-789")).thenReturn(Optional.of(orden));
        when(ejecucionRepository.save(any())).thenReturn(ejecucionGuardada);
        when(ejecucionRepository.findByOrdenId(15L)).thenReturn(List.of(ejecucionGuardada));
        when(portafolioClient.actualizarPosicion(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new BigDecimal("100.00"));

        ProcesarEjecucionUseCase.ProcesarEjecucionCommand cmd = new ProcesarEjecucionUseCase.ProcesarEjecucionCommand(
                null, "exec-222", "broker-789", 5, new BigDecimal("100.00"), null, null
        );

        EjecucionOrden resultado = service.ejecutar(cmd);

        assertEquals(15L, resultado.getOrdenId());
        verify(ordenRepository).findByBrokerOrderId("broker-789");
    }

    @Test
    void excepcionEnCoordinacionM2M6NoFallaEjecucion() throws Exception {
        Orden orden = new Orden();
        orden.setId(16L);
        orden.setUsuarioId(7L);
        orden.setTicker("INTC");
        orden.setLadoOrden(LadoOrden.COMPRA);
        orden.setCantidad(10);
        orden.setConfiguracionComisionId(5L);
        orden.setReservaFondosId(200L);

        EjecucionOrden ejecucionGuardada = new EjecucionOrden();
        ejecucionGuardada.setId(7L);
        ejecucionGuardada.setOrdenId(16L);
        ejecucionGuardada.setCantidad(10);

        when(ejecucionRepository.findByBrokerExecutionId("exec-333")).thenReturn(Optional.empty());
        when(ordenRepository.findById(16L)).thenReturn(Optional.of(orden));
        ConfiguracionComision comisionCfg = new ConfiguracionComision();
        comisionCfg.setId(5L);
        comisionCfg.setPorcentaje(new BigDecimal("0.02"));
        when(comisionRepository.findById(5L)).thenReturn(Optional.of(comisionCfg));
        when(ejecucionRepository.save(any())).thenReturn(ejecucionGuardada);
        when(ejecucionRepository.findByOrdenId(16L)).thenReturn(List.of(ejecucionGuardada));
        doThrow(new RuntimeException("M2/M6 error")).when(fondosClient).confirmarReserva(anyLong(), anyLong(), anyString(), any(), anyBoolean(), any());

        ProcesarEjecucionUseCase.ProcesarEjecucionCommand cmd = new ProcesarEjecucionUseCase.ProcesarEjecucionCommand(
                16L, "exec-333", null, 10, new BigDecimal("30.00"), null, null
        );

        EjecucionOrden resultado = service.ejecutar(cmd);

        assertEquals(7L, resultado.getId());
        assertEquals(EstadoOrden.EJECUTADA, orden.getEstadoOrden());
        verify(fondosClient).confirmarReserva(eq(200L), eq(7L), eq("exec-333"), any(), eq(true), eq(1));
        verifyNoInteractions(confirmacionRepository);
        verify(publicador).publicarOrdenEjecutada(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyBoolean(), any());
    }

    @Test
    void outboxConObjectMapperExcepcionNoFalla() throws Exception {
        Orden orden = new Orden();
        orden.setId(17L);
        orden.setUsuarioId(8L);
        orden.setTicker("AMZN");
        orden.setLadoOrden(LadoOrden.VENTA);
        orden.setCantidad(3);
        orden.setReservaFondosId(null);

        EjecucionOrden ejecucionGuardada = new EjecucionOrden();
        ejecucionGuardada.setId(8L);
        ejecucionGuardada.setOrdenId(17L);
        ejecucionGuardada.setCantidad(3);
        ejecucionGuardada.setPrecioEjecucion(new BigDecimal("3000.00"));
        ejecucionGuardada.setComision(new BigDecimal("5.00"));
        ejecucionGuardada.setEjecutadaEn(Instant.now());

        when(ejecucionRepository.findByBrokerExecutionId("exec-444")).thenReturn(Optional.empty());
        when(ordenRepository.findById(17L)).thenReturn(Optional.of(orden));
        when(ejecucionRepository.save(any())).thenReturn(ejecucionGuardada);
        when(ejecucionRepository.findByOrdenId(17L)).thenReturn(List.of(ejecucionGuardada));
        when(portafolioClient.actualizarPosicion(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new BigDecimal("3000.00"));
        when(objectMapper.writeValueAsString(any(Map.class))).thenThrow(new RuntimeException("JSON error"));

        ProcesarEjecucionUseCase.ProcesarEjecucionCommand cmd = new ProcesarEjecucionUseCase.ProcesarEjecucionCommand(
                17L, "exec-444", null, 3, new BigDecimal("3000.00"), null, null
        );

        EjecucionOrden resultado = service.ejecutar(cmd);

        assertEquals(8L, resultado.getId());
        verifyNoInteractions(outboxRepository);
    }

    @Test
    void ventaSinConfirmacionPortafolioRetornaNull() throws Exception {
        Orden orden = new Orden();
        orden.setId(18L);
        orden.setUsuarioId(9L);
        orden.setTicker("META");
        orden.setLadoOrden(LadoOrden.VENTA);
        orden.setCantidad(7);
        orden.setReservaFondosId(null);

        EjecucionOrden ejecucionGuardada = new EjecucionOrden();
        ejecucionGuardada.setId(9L);
        ejecucionGuardada.setOrdenId(18L);
        ejecucionGuardada.setCantidad(7);

        when(ejecucionRepository.findByBrokerExecutionId("exec-555")).thenReturn(Optional.empty());
        when(ordenRepository.findById(18L)).thenReturn(Optional.of(orden));
        when(ejecucionRepository.save(any())).thenReturn(ejecucionGuardada);
        when(ejecucionRepository.findByOrdenId(18L)).thenReturn(List.of(ejecucionGuardada));
        when(portafolioClient.actualizarPosicion(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(null);

        ProcesarEjecucionUseCase.ProcesarEjecucionCommand cmd = new ProcesarEjecucionUseCase.ProcesarEjecucionCommand(
                18L, "exec-555", null, 7, new BigDecimal("400.00"), null, null
        );

        service.ejecutar(cmd);

        verify(confirmacionRepository, never()).save(argThat(c ->
                c.getServicioParticipante() == ServicioParticipante.PORTAFOLIO
        ));
        verify(confirmacionRepository).save(argThat(c ->
                c.getServicioParticipante() == ServicioParticipante.FONDOS
        ));
    }

    @Test
    void compraSinReservaFondosNoLlamaFondosClient() throws Exception {
        Orden orden = new Orden();
        orden.setId(19L);
        orden.setUsuarioId(10L);
        orden.setTicker("GOOGL");
        orden.setLadoOrden(LadoOrden.COMPRA);
        orden.setCantidad(4);
        orden.setConfiguracionComisionId(5L);
        orden.setReservaFondosId(null);

        EjecucionOrden ejecucionGuardada = new EjecucionOrden();
        ejecucionGuardada.setId(10L);
        ejecucionGuardada.setOrdenId(19L);
        ejecucionGuardada.setCantidad(4);

        when(ejecucionRepository.findByBrokerExecutionId("exec-666")).thenReturn(Optional.empty());
        when(ordenRepository.findById(19L)).thenReturn(Optional.of(orden));
        ConfiguracionComision comisionCfg = new ConfiguracionComision();
        comisionCfg.setId(5L);
        comisionCfg.setPorcentaje(new BigDecimal("0.015"));
        when(comisionRepository.findById(5L)).thenReturn(Optional.of(comisionCfg));
        when(ejecucionRepository.save(any())).thenReturn(ejecucionGuardada);
        when(ejecucionRepository.findByOrdenId(19L)).thenReturn(List.of(ejecucionGuardada));
        when(portafolioClient.actualizarPosicion(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new BigDecimal("2800.00"));

        ProcesarEjecucionUseCase.ProcesarEjecucionCommand cmd = new ProcesarEjecucionUseCase.ProcesarEjecucionCommand(
                19L, "exec-666", null, 4, new BigDecimal("2800.00"), null, null
        );

        service.ejecutar(cmd);

        verifyNoInteractions(fondosClient);
        verify(portafolioClient).actualizarPosicion(
                eq("19"), eq("exec-666"), eq(10L), eq("GOOGL"), eq(LadoOrden.COMPRA), eq(4), eq(new BigDecimal("2800.00")), any(), any()
        );
    }

    @Test
    void compraConPortafolioRetornaNullNoGuardaConfirmacionPortafolio() throws Exception {
        Orden orden = new Orden();
        orden.setId(20L);
        orden.setUsuarioId(11L);
        orden.setTicker("BRK");
        orden.setLadoOrden(LadoOrden.COMPRA);
        orden.setCantidad(2);
        orden.setConfiguracionComisionId(5L);
        orden.setReservaFondosId(null);

        EjecucionOrden ejecucionGuardada = new EjecucionOrden();
        ejecucionGuardada.setId(11L);
        ejecucionGuardada.setOrdenId(20L);
        ejecucionGuardada.setCantidad(2);

        when(ejecucionRepository.findByBrokerExecutionId("exec-777")).thenReturn(Optional.empty());
        when(ordenRepository.findById(20L)).thenReturn(Optional.of(orden));
        ConfiguracionComision comisionCfg = new ConfiguracionComision();
        comisionCfg.setId(5L);
        comisionCfg.setPorcentaje(new BigDecimal("0.01"));
        when(comisionRepository.findById(5L)).thenReturn(Optional.of(comisionCfg));
        when(ejecucionRepository.save(any())).thenReturn(ejecucionGuardada);
        when(ejecucionRepository.findByOrdenId(20L)).thenReturn(List.of(ejecucionGuardada));
        when(portafolioClient.actualizarPosicion(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(null);

        ProcesarEjecucionUseCase.ProcesarEjecucionCommand cmd = new ProcesarEjecucionUseCase.ProcesarEjecucionCommand(
                20L, "exec-777", null, 2, new BigDecimal("500.00"), null, null
        );

        service.ejecutar(cmd);

        verify(confirmacionRepository, never()).save(argThat(c ->
                c.getServicioParticipante() == ServicioParticipante.PORTAFOLIO
        ));
    }

    @Test
    void ejecutadaEnNoNullUsaValorProporcionado() throws Exception {
        Orden orden = new Orden();
        orden.setId(21L);
        orden.setUsuarioId(12L);
        orden.setTicker("TSM");
        orden.setLadoOrden(LadoOrden.COMPRA);
        orden.setCantidad(5);
        orden.setConfiguracionComisionId(5L);
        orden.setReservaFondosId(null);

        Instant customTime = Instant.parse("2026-01-15T10:30:00Z");

        when(ejecucionRepository.findByBrokerExecutionId("exec-888")).thenReturn(Optional.empty());
        when(ordenRepository.findById(21L)).thenReturn(Optional.of(orden));
        ConfiguracionComision comisionCfg = new ConfiguracionComision();
        comisionCfg.setId(5L);
        comisionCfg.setPorcentaje(new BigDecimal("0.02"));
        when(comisionRepository.findById(5L)).thenReturn(Optional.of(comisionCfg));
        when(ejecucionRepository.save(any(EjecucionOrden.class))).thenAnswer(inv -> {
            EjecucionOrden e = inv.getArgument(0);
            e.setId(12L);
            return e;
        });
        when(ejecucionRepository.findByOrdenId(21L)).thenReturn(List.of());
        when(portafolioClient.actualizarPosicion(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new BigDecimal("100.00"));

        ProcesarEjecucionUseCase.ProcesarEjecucionCommand cmd = new ProcesarEjecucionUseCase.ProcesarEjecucionCommand(
                21L, "exec-888", null, 5, new BigDecimal("100.00"), null, customTime
        );

        EjecucionOrden resultado = service.ejecutar(cmd);

        assertEquals(customTime, resultado.getEjecutadaEn());
    }

    @Test
    void ordenEncontradaPorIdNoConsultaBrokerOrderId() throws Exception {
        Orden orden = new Orden();
        orden.setId(22L);
        orden.setUsuarioId(13L);
        orden.setTicker("NFLX");
        orden.setLadoOrden(LadoOrden.VENTA);
        orden.setCantidad(3);
        orden.setReservaFondosId(null);

        EjecucionOrden ejecucionGuardada = new EjecucionOrden();
        ejecucionGuardada.setId(13L);
        ejecucionGuardada.setOrdenId(22L);
        ejecucionGuardada.setCantidad(3);

        when(ejecucionRepository.findByBrokerExecutionId("exec-999")).thenReturn(Optional.empty());
        when(ordenRepository.findById(22L)).thenReturn(Optional.of(orden));
        when(ejecucionRepository.save(any())).thenReturn(ejecucionGuardada);
        when(ejecucionRepository.findByOrdenId(22L)).thenReturn(List.of(ejecucionGuardada));
        when(portafolioClient.actualizarPosicion(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new BigDecimal("400.00"));

        ProcesarEjecucionUseCase.ProcesarEjecucionCommand cmd = new ProcesarEjecucionUseCase.ProcesarEjecucionCommand(
                22L, "exec-999", "broker-should-not-be-used", 3, new BigDecimal("400.00"), null, null
        );

        service.ejecutar(cmd);

        verify(ordenRepository, never()).findByBrokerOrderId(anyString());
    }

    @Test
    void ordenNoEncontradaPorIdPeroSiPorBrokerOrderId() throws Exception {
        Orden orden = new Orden();
        orden.setId(23L);
        orden.setUsuarioId(14L);
        orden.setTicker("AMZN");
        orden.setLadoOrden(LadoOrden.COMPRA);
        orden.setCantidad(6);
        orden.setConfiguracionComisionId(5L);
        orden.setReservaFondosId(null);

        EjecucionOrden ejecucionGuardada = new EjecucionOrden();
        ejecucionGuardada.setId(14L);
        ejecucionGuardada.setOrdenId(23L);
        ejecucionGuardada.setCantidad(6);

        when(ejecucionRepository.findByBrokerExecutionId("exec-111")).thenReturn(Optional.empty());
        when(ordenRepository.findById(999L)).thenReturn(Optional.empty());
        when(ordenRepository.findByBrokerOrderId("broker-fallback")).thenReturn(Optional.of(orden));
        when(ejecucionRepository.save(any())).thenReturn(ejecucionGuardada);
        when(ejecucionRepository.findByOrdenId(23L)).thenReturn(List.of(ejecucionGuardada));
        when(portafolioClient.actualizarPosicion(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new BigDecimal("3000.00"));

        ProcesarEjecucionUseCase.ProcesarEjecucionCommand cmd = new ProcesarEjecucionUseCase.ProcesarEjecucionCommand(
                999L, "exec-111", "broker-fallback", 6, new BigDecimal("3000.00"), null, null
        );

        EjecucionOrden resultado = service.ejecutar(cmd);

        assertEquals(23L, resultado.getOrdenId());
        verify(ordenRepository).findById(999L);
        verify(ordenRepository).findByBrokerOrderId("broker-fallback");
    }

    @Test
    void ordenNoEncontradaPorIdNiPorBrokerOrderIdLanzaExcepcion() {
        when(ejecucionRepository.findByBrokerExecutionId("exec-222")).thenReturn(Optional.empty());
        when(ordenRepository.findById(998L)).thenReturn(Optional.empty());

        ProcesarEjecucionUseCase.ProcesarEjecucionCommand cmd = new ProcesarEjecucionUseCase.ProcesarEjecucionCommand(
                998L, "exec-222", null, 5, new BigDecimal("100.00"), null, null
        );

        assertThrows(IllegalArgumentException.class, () -> service.ejecutar(cmd));
        verify(ordenRepository).findById(998L);
        verify(ordenRepository, never()).findByBrokerOrderId(anyString());
    }

}

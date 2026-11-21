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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
        doNothing().when(fondosClient).confirmarReserva(eq(100L), eq("exec-123"), eq(new BigDecimal("1530.00")));
        when(portafolioClient.actualizarPosicion(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new BigDecimal("152.00"));

        ProcesarEjecucionUseCase.ProcesarEjecucionCommand cmd = new ProcesarEjecucionUseCase.ProcesarEjecucionCommand(
                10L, "exec-123", null, 10, new BigDecimal("150.00"), null, null
        );

        EjecucionOrden resultado = service.ejecutar(cmd);

        assertEquals(1L, resultado.getId());
        assertEquals(EstadoOrden.EJECUTADA, orden.getEstadoOrden());
        verify(fondosClient).confirmarReserva(eq(100L), eq("exec-123"), eq(new BigDecimal("1530.00")));
        verify(portafolioClient).actualizarPosicion(
                eq("10"), eq("exec-123"), eq(1L), eq("AAPL"), eq(LadoOrden.COMPRA), eq(10), eq(new BigDecimal("150.00")), any(), any()
        );
        verify(publicador).publicarOrdenEjecutada(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
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
        verify(publicador).publicarOrdenEjecutada(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
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

}

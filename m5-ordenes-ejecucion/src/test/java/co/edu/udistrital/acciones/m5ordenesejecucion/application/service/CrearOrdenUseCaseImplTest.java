package co.edu.udistrital.acciones.m5ordenesejecucion.application.service;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.CrearOrdenUseCase;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.AlpacaOrderPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.ConfiguracionComisionRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.DatosMercadoClientPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.FondosClientPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.HistorialEstadoOrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.OrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.OutboxEventoRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.PortafolioClientPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.PublicadorOrdenesPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.ConfiguracionComision;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EstadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.HistorialEstadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.LadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.Orden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.OutboxEvento;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.TipoActor;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.TipoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.VigenciaOrden;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrearOrdenUseCaseImplTest {

    @Mock
    OrdenRepositoryPort ordenRepository;

    @Mock
    ConfiguracionComisionRepositoryPort comisionRepository;

    @Mock
    HistorialEstadoOrdenRepositoryPort historialRepository;

    @Mock
    OutboxEventoRepositoryPort outboxRepository;

    @Mock
    FondosClientPort fondosClient;

    @Mock
    PortafolioClientPort portafolioClient;

    @Mock
    DatosMercadoClientPort datosMercadoClient;

    @Mock
    AlpacaOrderPort alpacaOrderPort;

    @Mock
    PublicadorOrdenesPort publicador;

    @Mock
    com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    private CrearOrdenUseCaseImpl service;

    @BeforeEach
    void setUp() {
        service = new CrearOrdenUseCaseImpl(
                ordenRepository, comisionRepository, historialRepository,
                outboxRepository, fondosClient, portafolioClient,
                datosMercadoClient, alpacaOrderPort, publicador, objectMapper
        );
    }

    @Test
    void crearOrdenMercadoCerradoRequiereEncolamiento() throws Exception {
        UUID clientOrderId = UUID.randomUUID();
        when(ordenRepository.findByClientOrderId(clientOrderId)).thenReturn(Optional.empty());

        ConfiguracionComision comision = new ConfiguracionComision();
        comision.setId(1L);
        comision.setMercadoCodigo("NYSE");
        comision.setPorcentaje(new BigDecimal("0.02"));
        comision.setVigenteDesde(Instant.now());
        comision.setVigenteHasta(Instant.now().plus(30, ChronoUnit.DAYS));
        when(comisionRepository.findVigentePorMercado(eq("NYSE"), any(Instant.class)))
                .thenReturn(Optional.of(comision));
        when(datosMercadoClient.obtenerPrecioEstimado("AAPL")).thenReturn(new BigDecimal("150.00"));
        when(datosMercadoClient.esMercadoAbierto("NYSE")).thenReturn(false);
        when(ordenRepository.save(any())).thenAnswer(inv -> {
            Orden o = inv.getArgument(0);
            o.setId(1L);
            return o;
        });
        when(objectMapper.writeValueAsString(any(Map.class))).thenReturn("{}");

        CrearOrdenUseCase.CrearOrdenCommand cmd = new CrearOrdenUseCase.CrearOrdenCommand(
                1L, 1L, "AAPL", TipoOrden.MARKET, LadoOrden.COMPRA,
                VigenciaOrden.DIA, 10, null, null, null, clientOrderId
        );

        Orden resultado = service.ejecutar(cmd);

        assertNotNull(resultado);
        assertEquals(EstadoOrden.PENDIENTE, resultado.getEstadoOrden());
        assertTrue(Boolean.TRUE.equals(resultado.getRequiereEncolamiento()));
        verify(outboxRepository).save(any());
        verifyNoInteractions(alpacaOrderPort);
        verifyNoInteractions(fondosClient);
        verifyNoInteractions(portafolioClient);
    }

    @Test
    void crearOrdenDuplicadaPorIdempotenciaRetornaExistente() {
        UUID clientOrderId = UUID.randomUUID();
        Orden existente = new Orden();
        existente.setId(5L);
        existente.setEstadoOrden(EstadoOrden.PENDIENTE);

        when(ordenRepository.findByClientOrderId(clientOrderId)).thenReturn(Optional.of(existente));

        CrearOrdenUseCase.CrearOrdenCommand cmd = new CrearOrdenUseCase.CrearOrdenCommand(
                1L, 1L, "AAPL", TipoOrden.MARKET, LadoOrden.COMPRA,
                VigenciaOrden.DIA, 10, null, null, null, clientOrderId
        );

        Orden resultado = service.ejecutar(cmd);

        assertEquals(5L, resultado.getId());
        verify(ordenRepository, never()).save(any());
        verifyNoInteractions(publicador);
    }

    @Test
    void crearOrdenSinConfiguracionComisionLanzaExcepcion() {
        UUID clientOrderId = UUID.randomUUID();
        when(ordenRepository.findByClientOrderId(clientOrderId)).thenReturn(Optional.empty());
        when(comisionRepository.findVigentePorMercado(eq("NYSE"), any(Instant.class))).thenReturn(Optional.empty());
        when(comisionRepository.findVigentes(any(Instant.class))).thenReturn(java.util.List.of());

        CrearOrdenUseCase.CrearOrdenCommand cmd = new CrearOrdenUseCase.CrearOrdenCommand(
                1L, 1L, "AAPL", TipoOrden.MARKET, LadoOrden.COMPRA,
                VigenciaOrden.DIA, 10, null, null, null, clientOrderId
        );

        assertThrows(IllegalStateException.class, () -> service.ejecutar(cmd));
    }
}

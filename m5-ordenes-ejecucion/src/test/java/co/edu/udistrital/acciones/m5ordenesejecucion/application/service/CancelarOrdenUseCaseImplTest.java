package co.edu.udistrital.acciones.m5ordenesejecucion.application.service;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.CancelarOrdenUseCase;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.AlpacaOrderPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.FondosClientPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.HistorialEstadoOrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.OrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.OutboxEventoRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.PortafolioClientPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.PublicadorOrdenesPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EstadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.HistorialEstadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.Orden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.OutboxEvento;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.TipoActor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CancelarOrdenUseCaseImplTest {

    @Mock
    OrdenRepositoryPort ordenRepository;

    @Mock
    HistorialEstadoOrdenRepositoryPort historialRepository;

    @Mock
    OutboxEventoRepositoryPort outboxRepository;

    @Mock
    FondosClientPort fondosClient;

    @Mock
    PortafolioClientPort portafolioClient;

    @Mock
    AlpacaOrderPort alpacaOrderPort;

    @Mock
    PublicadorOrdenesPort publicador;

    @Mock
    com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    private CancelarOrdenUseCaseImpl service;

    @BeforeEach
    void setUp() {
        service = new CancelarOrdenUseCaseImpl(
                ordenRepository, historialRepository, outboxRepository,
                fondosClient, portafolioClient, alpacaOrderPort, publicador, objectMapper
        );
    }

    @Test
    void cancelarOrdenPendienteExitosa() throws Exception {
        Orden orden = new Orden();
        orden.setId(1L);
        orden.setUsuarioId(1L);
        orden.setEstadoOrden(EstadoOrden.PENDIENTE);
        orden.setActualizadaEn(Instant.now());

        when(ordenRepository.findById(1L)).thenReturn(java.util.Optional.of(orden));
        when(ordenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.ejecutar(1L, 1L, "cancelar por usuario");

        assertEquals(EstadoOrden.CANCELADA, orden.getEstadoOrden());
        verify(publicador).publicarOrdenCancelada(1L, "cancelar por usuario");
    }

    @Test
    void cancelarOrdenYaEjecutadaLanzaExcepcion() {
        Orden orden = new Orden();
        orden.setId(1L);
        orden.setUsuarioId(1L);
        orden.setEstadoOrden(EstadoOrden.EJECUTADA);

        when(ordenRepository.findById(1L)).thenReturn(java.util.Optional.of(orden));

        assertThrows(IllegalStateException.class, () -> service.ejecutar(1L, 1L, "cancelar"));
        verifyNoInteractions(publicador);
    }

    @Test
    void cancelarOrdenInexistenteLanzaExcepcion() {
        when(ordenRepository.findById(999L)).thenReturn(java.util.Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.ejecutar(999L, 1L, "cancelar"));
    }

    @Test
    void cancelarOrdenYaCanceladaEsIdempotente() {
        Orden orden = new Orden();
        orden.setId(1L);
        orden.setUsuarioId(1L);
        orden.setEstadoOrden(EstadoOrden.CANCELADA);

        when(ordenRepository.findById(1L)).thenReturn(java.util.Optional.of(orden));

        service.ejecutar(1L, 1L, "cancelar");

        verifyNoInteractions(publicador);
        verify(ordenRepository, never()).save(any());
    }

    @Test
    void cancelarOrdenUsuarioAjenoLanzaExcepcion() {
        Orden orden = new Orden();
        orden.setId(1L);
        orden.setUsuarioId(99L);
        orden.setEstadoOrden(EstadoOrden.PENDIENTE);

        when(ordenRepository.findById(1L)).thenReturn(java.util.Optional.of(orden));

        assertThrows(IllegalArgumentException.class, () -> service.ejecutar(1L, 1L, "cancelar"));
    }
}

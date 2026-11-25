package co.edu.udistrital.acciones.m5ordenesejecucion.application.service;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.CancelarOrdenUseCase;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.AlpacaOrderPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.FondosClientPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.EjecucionOrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.ConfirmacionEjecucionRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.HistorialEstadoOrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.OrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.OutboxEventoRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.PortafolioClientPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.PublicadorOrdenesPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EstadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EjecucionOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.ConfirmacionEjecucion;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.LadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.ServicioParticipante;
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

    @Mock
    EjecucionOrdenRepositoryPort ejecucionRepository;

    @Mock
    ConfirmacionEjecucionRepositoryPort confirmacionRepository;

    private CancelarOrdenUseCaseImpl service;

    @BeforeEach
    void setUp() {
        service = new CancelarOrdenUseCaseImpl(
                ordenRepository, historialRepository, outboxRepository,
                fondosClient, portafolioClient, alpacaOrderPort, publicador, objectMapper,
                ejecucionRepository, confirmacionRepository
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
        verify(publicador).publicarOrdenCancelada(any(UUID.class), eq(1L), any(), eq("cancelar por usuario"));
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

    @Test
    void liberaReservasConLaIdentidadDelDuenoDeLaOrden() {
        Orden orden = new Orden();
        orden.setId(5L);
        orden.setUsuarioId(42L);
        orden.setEstadoOrden(EstadoOrden.ENVIADA);
        orden.setReservaFondosId(7L);
        orden.setReservaPosicionId(8L);
        when(ordenRepository.findById(5L)).thenReturn(java.util.Optional.of(orden));

        service.ejecutar(5L, 42L, "Cancelada");

        verify(fondosClient).liberarReserva(7L, 42L, "Cancelada");
        verify(portafolioClient).liberarReserva(8L, 42L, "Cancelada");
    }

    @Test
    void cancelacionRechazadaPorBrokerConservaEstadoYReservasTrasFillParcial() {
        Orden orden = new Orden();
        orden.setId(5L);
        orden.setUsuarioId(42L);
        orden.setEstadoOrden(EstadoOrden.PARCIALMENTE_EJECUTADA);
        orden.setBrokerOrderId("broker-5");
        orden.setReservaFondosId(7L);
        orden.setReservaPosicionId(8L);
        when(ordenRepository.findById(5L)).thenReturn(java.util.Optional.of(orden));
        doThrow(new IllegalStateException("Alpaca no canceló")).when(alpacaOrderPort).cancelarOrden("broker-5");

        assertThrows(IllegalStateException.class, () -> service.ejecutar(5L, 42L, "Cancelada"));

        assertEquals(EstadoOrden.PARCIALMENTE_EJECUTADA, orden.getEstadoOrden());
        verifyNoInteractions(fondosClient, portafolioClient, publicador, historialRepository, outboxRepository);
        verify(ordenRepository, never()).save(any());
    }

    @Test
    void cancelacionTrasFillParcialLiberaElRestanteDespuesDelBroker() {
        Orden orden = new Orden();
        orden.setId(5L);
        orden.setUsuarioId(42L);
        orden.setEstadoOrden(EstadoOrden.PARCIALMENTE_EJECUTADA);
        orden.setLadoOrden(LadoOrden.COMPRA);
        orden.setBrokerOrderId("broker-5");
        orden.setReservaFondosId(7L);
        when(ordenRepository.findById(5L)).thenReturn(java.util.Optional.of(orden));
        when(alpacaOrderPort.cancelarOrden("broker-5")).thenReturn(
                new AlpacaOrderPort.AlpacaOrderResponse("broker-5", null, "canceled", "AAPL", 10, null, 4));
        EjecucionOrden parcial = new EjecucionOrden();
        parcial.setId(1L);
        parcial.setCantidad(4);
        when(ejecucionRepository.findByOrdenId(5L)).thenReturn(java.util.List.of(parcial));
        when(confirmacionRepository.findByEjecucionIdAndServicio(1L, ServicioParticipante.FONDOS))
                .thenReturn(java.util.Optional.of(new ConfirmacionEjecucion()));
        when(confirmacionRepository.findByEjecucionIdAndServicio(1L, ServicioParticipante.PORTAFOLIO))
                .thenReturn(java.util.Optional.of(new ConfirmacionEjecucion()));

        service.ejecutar(5L, 42L, "Cancelada");

        var secuencia = inOrder(alpacaOrderPort, fondosClient, ordenRepository);
        secuencia.verify(alpacaOrderPort).cancelarOrden("broker-5");
        secuencia.verify(fondosClient).liberarReserva(7L, 42L, "Cancelada");
        secuencia.verify(ordenRepository).save(orden);
        assertEquals(EstadoOrden.CANCELADA, orden.getEstadoOrden());
    }

    @Test
    void cancelacionTerminalConFillsAunNoRegistradosConservaLasReservas() {
        Orden orden = parcialConBroker();
        when(ordenRepository.findById(5L)).thenReturn(java.util.Optional.of(orden));
        when(alpacaOrderPort.cancelarOrden("broker-5")).thenReturn(
                new AlpacaOrderPort.AlpacaOrderResponse("broker-5", null, "canceled", "AAPL", 10, null, 4));

        assertThrows(IllegalStateException.class, () -> service.ejecutar(5L, 42L, "Cancelada"));

        assertEquals(EstadoOrden.PARCIALMENTE_EJECUTADA, orden.getEstadoOrden());
        verifyNoInteractions(fondosClient, portafolioClient, publicador);
        verify(ordenRepository, never()).save(any());
    }

    @Test
    void fillRegistradoPeroPendienteDeAplicarEnServiciosNoLiberaReservas() {
        Orden orden = parcialConBroker();
        when(ordenRepository.findById(5L)).thenReturn(java.util.Optional.of(orden));
        when(alpacaOrderPort.cancelarOrden("broker-5")).thenReturn(
                new AlpacaOrderPort.AlpacaOrderResponse("broker-5", null, "canceled", "AAPL", 10, null, 4));
        EjecucionOrden parcial = new EjecucionOrden();
        parcial.setId(1L);
        parcial.setCantidad(4);
        when(ejecucionRepository.findByOrdenId(5L)).thenReturn(java.util.List.of(parcial));

        assertThrows(IllegalStateException.class, () -> service.ejecutar(5L, 42L, "Cancelada"));

        verifyNoInteractions(fondosClient, portafolioClient, publicador);
        assertEquals(EstadoOrden.PARCIALMENTE_EJECUTADA, orden.getEstadoOrden());
    }

    @Test
    void snapshotDeCancelacionSinFilledQtyConservaLasReservas() {
        Orden orden = parcialConBroker();
        when(ordenRepository.findById(5L)).thenReturn(java.util.Optional.of(orden));
        when(alpacaOrderPort.cancelarOrden("broker-5")).thenReturn(
                new AlpacaOrderPort.AlpacaOrderResponse("broker-5", null, "canceled", "AAPL", 10, null, null));

        assertThrows(IllegalStateException.class, () -> service.ejecutar(5L, 42L, "Cancelada"));

        verifyNoInteractions(fondosClient, portafolioClient, publicador);
    }

    private static Orden parcialConBroker() {
        Orden orden = new Orden();
        orden.setId(5L);
        orden.setUsuarioId(42L);
        orden.setEstadoOrden(EstadoOrden.PARCIALMENTE_EJECUTADA);
        orden.setLadoOrden(LadoOrden.COMPRA);
        orden.setBrokerOrderId("broker-5");
        orden.setReservaFondosId(7L);
        orden.setReservaPosicionId(8L);
        return orden;
    }
}

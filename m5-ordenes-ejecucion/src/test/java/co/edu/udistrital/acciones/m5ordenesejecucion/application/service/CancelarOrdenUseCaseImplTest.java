package co.edu.udistrital.acciones.m5ordenesejecucion.application.service;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.CancelarOrdenUseCase;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.OrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.PublicadorOrdenesPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EstadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.Orden;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CancelarOrdenUseCaseImplTest {

    @Mock
    OrdenRepositoryPort ordenRepository;

    @Mock
    PublicadorOrdenesPort publicador;

    private CancelarOrdenUseCaseImpl service;

    @BeforeEach
    void setUp() {
        service = new CancelarOrdenUseCaseImpl(ordenRepository, publicador);
    }

    @Test
    void cancelarOrdenPendienteExitosa() {
        Orden orden = new Orden();
        orden.setId(1L);
        orden.setEstado(EstadoOrden.PENDIENTE);
        orden.setActualizadoEn(Instant.now());

        when(ordenRepository.findById(1L)).thenReturn(java.util.Optional.of(orden));
        when(ordenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.ejecutar(1L, "cancelar por usuario");

        assertEquals(EstadoOrden.CANCELADA, orden.getEstado());
        verify(publicador).publicarOrdenCancelada(1L, "cancelar por usuario");
    }

    @Test
    void cancelarOrdenNoPendienteLanzaExcepcion() {
        Orden orden = new Orden();
        orden.setId(1L);
        orden.setEstado(EstadoOrden.EJECUTADA);

        when(ordenRepository.findById(1L)).thenReturn(java.util.Optional.of(orden));

        assertThrows(IllegalArgumentException.class, () -> service.ejecutar(1L, "cancelar"));
        verifyNoInteractions(publicador);
    }

    @Test
    void cancelarOrdenInexistenteLanzaExcepcion() {
        when(ordenRepository.findById(999L)).thenReturn(java.util.Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.ejecutar(999L, "cancelar"));
    }
}

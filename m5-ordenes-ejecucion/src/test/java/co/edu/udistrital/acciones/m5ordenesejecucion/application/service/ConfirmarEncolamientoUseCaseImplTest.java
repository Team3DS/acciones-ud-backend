package co.edu.udistrital.acciones.m5ordenesejecucion.application.service;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.ConfirmarEncolamientoUseCase;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.HistorialEstadoOrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.OrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EstadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.Orden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.TipoActor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfirmarEncolamientoUseCaseImplTest {

    @Mock
    OrdenRepositoryPort ordenRepository;

    @Mock
    HistorialEstadoOrdenRepositoryPort historialRepository;

    private ConfirmarEncolamientoUseCaseImpl service;

    @BeforeEach
    void setUp() {
        service = new ConfirmarEncolamientoUseCaseImpl(ordenRepository, historialRepository);
    }

    @Test
    void confirmarEncolamientoExitoso() {
        Orden orden = new Orden();
        orden.setId(1L);
        orden.setUsuarioId(1L);
        orden.setEstadoOrden(EstadoOrden.PENDIENTE);
        orden.setRequiereEncolamiento(true);
        orden.setActualizadaEn(Instant.now());

        when(ordenRepository.findById(1L)).thenReturn(Optional.of(orden));
        when(ordenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Orden resultado = service.ejecutar(1L, 1L);

        assertNotNull(resultado.getConfirmadoEncolamientoEn());
        verify(ordenRepository).save(orden);
        verify(historialRepository).save(any());
    }

    @Test
    void ordenNoEncontradaLanzaExcepcion() {
        when(ordenRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.ejecutar(999L, 1L));
        verifyNoInteractions(historialRepository);
    }

    @Test
    void usuarioAjenoLanzaExcepcion() {
        Orden orden = new Orden();
        orden.setId(1L);
        orden.setUsuarioId(99L);
        orden.setEstadoOrden(EstadoOrden.PENDIENTE);
        orden.setRequiereEncolamiento(true);

        when(ordenRepository.findById(1L)).thenReturn(Optional.of(orden));

        assertThrows(IllegalArgumentException.class, () -> service.ejecutar(1L, 1L));
        verifyNoInteractions(historialRepository);
    }

    @Test
    void ordenSinRequerirEncolamientoLanzaExcepcion() {
        Orden orden = new Orden();
        orden.setId(1L);
        orden.setUsuarioId(1L);
        orden.setEstadoOrden(EstadoOrden.PENDIENTE);
        orden.setRequiereEncolamiento(false);

        when(ordenRepository.findById(1L)).thenReturn(Optional.of(orden));

        assertThrows(IllegalStateException.class, () -> service.ejecutar(1L, 1L));
        verifyNoInteractions(historialRepository);
    }

    @Test
    void ordenNoPendienteLanzaExcepcion() {
        Orden orden = new Orden();
        orden.setId(1L);
        orden.setUsuarioId(1L);
        orden.setEstadoOrden(EstadoOrden.EJECUTADA);
        orden.setRequiereEncolamiento(true);

        when(ordenRepository.findById(1L)).thenReturn(Optional.of(orden));

        assertThrows(IllegalStateException.class, () -> service.ejecutar(1L, 1L));
        verifyNoInteractions(historialRepository);
    }
}

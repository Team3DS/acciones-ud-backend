package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.in.scheduling;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.SincronizarEjecucionesUseCase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class SincronizadorEjecucionesTest {

    private final SincronizarEjecucionesUseCase useCase = mock(SincronizarEjecucionesUseCase.class);
    private final SincronizadorEjecuciones sincronizador = new SincronizadorEjecuciones(useCase);

    @Test
    void ejecutaUnaPasadaConEjecucionesRegistradas() {
        when(useCase.sincronizar()).thenReturn(2);

        sincronizador.sincronizar();

        verify(useCase).sincronizar();
    }

    @Test
    void unaPasadaSinEjecucionesNoFalla() {
        when(useCase.sincronizar()).thenReturn(0);

        assertDoesNotThrow(sincronizador::sincronizar);
        verify(useCase).sincronizar();
    }

    @Test
    void unErrorNoDetieneElProgramador() {
        when(useCase.sincronizar()).thenThrow(new IllegalStateException("base de datos caída"));

        assertDoesNotThrow(sincronizador::sincronizar);
    }
}

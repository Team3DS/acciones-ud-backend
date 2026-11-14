package co.edu.udistrital.acciones.m2cuentasfondos.unit;

import co.edu.udistrital.acciones.m2cuentasfondos.application.usecase.ExpirarReservasUseCase;
import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.adapter.in.scheduling.ExpiracionReservasScheduler;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExpiracionReservasSchedulerTest {

    private final ExpirarReservasUseCase casoDeUso = mock(ExpirarReservasUseCase.class);
    private final ExpiracionReservasScheduler scheduler = new ExpiracionReservasScheduler(casoDeUso);

    @Test
    void delegaLaExpiracionEnElCasoDeUso() {
        when(casoDeUso.ejecutar()).thenReturn(2);

        scheduler.expirarPendientes();

        verify(casoDeUso).ejecutar();
    }

    @Test
    void unFalloNoPropagaLaExcepcionAlScheduler() {
        when(casoDeUso.ejecutar()).thenThrow(new IllegalStateException("base de datos no disponible"));

        assertThatCode(scheduler::expirarPendientes).doesNotThrowAnyException();
        verify(casoDeUso).ejecutar();
    }
}

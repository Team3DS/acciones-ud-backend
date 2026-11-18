package co.edu.udistrital.acciones.m6portafolio.unit;

import co.edu.udistrital.acciones.m6portafolio.application.usecase.ValorarPosicionesUseCase;
import co.edu.udistrital.acciones.m6portafolio.infrastructure.adapter.in.scheduling.ValoracionScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValoracionSchedulerTest {

    @Mock
    private ValorarPosicionesUseCase casoUso;

    @Test
    void delegaEnElCasoDeUsoDeValoracion() {
        when(casoUso.ejecutar()).thenReturn(2);

        new ValoracionScheduler(casoUso).valorar();

        verify(casoUso).ejecutar();
    }

    @Test
    void noPropagaLasExcepcionesDelCasoDeUso() {
        when(casoUso.ejecutar()).thenThrow(new IllegalStateException("fallo"));

        ValoracionScheduler scheduler = new ValoracionScheduler(casoUso);

        assertThatCode(scheduler::valorar).doesNotThrowAnyException();
    }
}

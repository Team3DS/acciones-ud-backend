package co.edu.udistrital.acciones.m4datosmercado.unit;

import co.edu.udistrital.acciones.m4datosmercado.application.port.in.ConsultarHorariosUseCase;
import co.edu.udistrital.acciones.m4datosmercado.application.port.out.CatalogoClientPort;
import co.edu.udistrital.acciones.m4datosmercado.application.usecase.ConsultarHorariosUseCaseImpl;
import co.edu.udistrital.acciones.m4datosmercado.domain.EstadoMercado;
import co.edu.udistrital.acciones.m4datosmercado.domain.HorarioMercado;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsultarHorariosUseCaseImplTest {

    @Mock
    CatalogoClientPort catalogoPort;

    ConsultarHorariosUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ConsultarHorariosUseCaseImpl(catalogoPort);
    }

    @Test
    void obtenerHorarios() {
        HorarioMercado h = new HorarioMercado("XNYS", 1, LocalTime.of(9, 30), LocalTime.of(16, 0), LocalTime.of(4, 0), LocalTime.of(20, 0));
        when(catalogoPort.obtenerHorariosMercado("XNYS")).thenReturn(List.of(h));

        List<HorarioMercado> result = useCase.obtenerHorarios("XNYS");

        assertEquals(1, result.size());
        assertEquals("XNYS", result.get(0).getMercadoMic());
    }

    @Test
    void obtenerEstado() {
        Instant now = Instant.now();
        when(catalogoPort.obtenerEstadoMercado("XNYS", now)).thenReturn(EstadoMercado.ABIERTO);

        EstadoMercado result = useCase.obtenerEstado("XNYS", now);

        assertEquals(EstadoMercado.ABIERTO, result);
    }
}
package co.edu.udistrital.acciones.m4datosmercado.application.port.in;

import co.edu.udistrital.acciones.m4datosmercado.domain.EstadoMercado;
import co.edu.udistrital.acciones.m4datosmercado.domain.HorarioMercado;

import java.time.Instant;
import java.util.List;

public interface ConsultarHorariosUseCase {
    List<HorarioMercado> obtenerHorarios(String mercadoMic);
    EstadoMercado obtenerEstado(String mercadoMic, Instant instant);
}
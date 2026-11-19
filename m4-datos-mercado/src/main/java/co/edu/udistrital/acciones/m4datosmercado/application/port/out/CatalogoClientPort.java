package co.edu.udistrital.acciones.m4datosmercado.application.port.out;

import co.edu.udistrital.acciones.m4datosmercado.domain.EstadoMercado;
import co.edu.udistrital.acciones.m4datosmercado.domain.HorarioMercado;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CatalogoClientPort {
    boolean validarSimbolo(String symbol);
    Optional<HorarioMercado> obtenerHorarioMercado(String mercadoMic);
    List<HorarioMercado> obtenerHorariosMercado(String mercadoMic);
    EstadoMercado obtenerEstadoMercado(String mercadoMic, Instant instant);
}
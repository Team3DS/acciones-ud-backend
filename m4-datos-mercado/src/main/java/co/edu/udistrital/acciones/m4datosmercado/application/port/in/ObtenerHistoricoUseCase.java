package co.edu.udistrital.acciones.m4datosmercado.application.port.in;

import co.edu.udistrital.acciones.m4datosmercado.domain.HistoricoCotizacion;
import co.edu.udistrital.acciones.m4datosmercado.domain.Intervalo;

import java.time.Instant;
import java.util.List;

public interface ObtenerHistoricoUseCase {
    List<HistoricoCotizacion> obtenerHistorico(String symbol, Instant from, Instant to, Intervalo intervalo);
}
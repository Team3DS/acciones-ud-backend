package co.edu.udistrital.acciones.m4datosmercado.application.port.out;

import co.edu.udistrital.acciones.m4datosmercado.domain.HistoricoCotizacion;
import co.edu.udistrital.acciones.m4datosmercado.domain.Intervalo;

import java.time.Instant;
import java.util.List;

public interface HistoricoRepositoryPort {
    List<HistoricoCotizacion> findBySymbolAndIntervaloAndTimestampBetween(String symbol, Intervalo intervalo, Instant from, Instant to);
    void saveAll(List<HistoricoCotizacion> historicos);
    boolean existsBySymbolAndIntervaloAndTimestamp(String symbol, Intervalo intervalo, Instant timestamp);
}
package co.edu.udistrital.acciones.m4datosmercado.application.port.out;

import co.edu.udistrital.acciones.m4datosmercado.domain.HistoricoCotizacion;
import java.util.List;

public interface HistoricoRepositoryPort {
    List<HistoricoCotizacion> findBySymbol(String symbol);
    void saveAll(List<HistoricoCotizacion> historicos);
}

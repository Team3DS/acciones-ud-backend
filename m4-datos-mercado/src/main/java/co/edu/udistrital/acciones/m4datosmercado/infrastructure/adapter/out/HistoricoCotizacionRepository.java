package co.edu.udistrital.acciones.m4datosmercado.infrastructure.adapter.out;

import co.edu.udistrital.acciones.m4datosmercado.domain.HistoricoCotizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HistoricoCotizacionRepository extends JpaRepository<HistoricoCotizacion, Long> {
    List<HistoricoCotizacion> findBySymbol(String symbol);
}

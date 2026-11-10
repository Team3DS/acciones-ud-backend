package co.edu.udistrital.acciones.m4datosmercado.infrastructure.adapter.out;

import co.edu.udistrital.acciones.m4datosmercado.application.port.out.HistoricoRepositoryPort;
import co.edu.udistrital.acciones.m4datosmercado.domain.HistoricoCotizacion;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JpaHistoricoRepositoryAdapter implements HistoricoRepositoryPort {
    private final HistoricoCotizacionRepository repo;

    public JpaHistoricoRepositoryAdapter(HistoricoCotizacionRepository repo) {
        this.repo = repo;
    }

    @Override
    public List<HistoricoCotizacion> findBySymbol(String symbol) {
        return repo.findBySymbol(symbol);
    }

    @Override
    public void saveAll(List<HistoricoCotizacion> historicos) {
        repo.saveAll(historicos);
    }
}

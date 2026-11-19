package co.edu.udistrital.acciones.m4datosmercado.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m4datosmercado.application.port.out.HistoricoRepositoryPort;
import co.edu.udistrital.acciones.m4datosmercado.domain.HistoricoCotizacion;
import co.edu.udistrital.acciones.m4datosmercado.domain.Intervalo;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class JpaHistoricoRepositoryAdapter implements HistoricoRepositoryPort {

    private final JpaHistoricoRepository repository;

    public JpaHistoricoRepositoryAdapter(JpaHistoricoRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistoricoCotizacion> findBySymbolAndIntervaloAndTimestampBetween(String symbol, Intervalo intervalo, Instant from, Instant to) {
        return repository.findBySymbolAndIntervaloAndTimestampBetween(symbol, intervalo, from, to);
    }

    @Override
    @Transactional
    public void saveAll(List<HistoricoCotizacion> historicos) {
        for (HistoricoCotizacion historico : historicos) {
            if (!repository.existsBySymbolAndIntervaloAndTimestamp(historico.getSymbol(), historico.getIntervalo(), historico.getTimestamp())) {
                repository.save(historico);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsBySymbolAndIntervaloAndTimestamp(String symbol, Intervalo intervalo, Instant timestamp) {
        return repository.existsBySymbolAndIntervaloAndTimestamp(symbol, intervalo, timestamp);
    }
}
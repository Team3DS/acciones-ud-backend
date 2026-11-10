package co.edu.udistrital.acciones.m4datosmercado.application.usecase;

import co.edu.udistrital.acciones.m4datosmercado.application.port.out.AlpacaMarketDataPort;
import co.edu.udistrital.acciones.m4datosmercado.application.port.out.HistoricoRepositoryPort;
import co.edu.udistrital.acciones.m4datosmercado.domain.HistoricoCotizacion;
import co.edu.udistrital.acciones.m4datosmercado.domain.Intervalo;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ObtenerHistoricoUseCase {
    private final AlpacaMarketDataPort alpaca;
    private final HistoricoRepositoryPort repo;

    public ObtenerHistoricoUseCase(AlpacaMarketDataPort alpaca, HistoricoRepositoryPort repo) {
        this.alpaca = alpaca;
        this.repo = repo;
    }

    public List<HistoricoCotizacion> execute(String symbol, Instant from, Instant to, Intervalo intervalo) {
        return alpaca.obtenerHistorico(symbol, from, to, intervalo);
    }
}

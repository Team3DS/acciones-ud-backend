package co.edu.udistrital.acciones.m4datosmercado.application.usecase;

import co.edu.udistrital.acciones.m4datosmercado.application.exception.SimboloInvalidoException;
import co.edu.udistrital.acciones.m4datosmercado.application.port.in.ObtenerHistoricoUseCase;
import co.edu.udistrital.acciones.m4datosmercado.application.port.out.AlpacaMarketDataPort;
import co.edu.udistrital.acciones.m4datosmercado.application.port.out.CatalogoClientPort;
import co.edu.udistrital.acciones.m4datosmercado.application.port.out.HistoricoRepositoryPort;
import co.edu.udistrital.acciones.m4datosmercado.domain.HistoricoCotizacion;
import co.edu.udistrital.acciones.m4datosmercado.domain.Intervalo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ObtenerHistoricoUseCaseImpl implements ObtenerHistoricoUseCase {

    private final HistoricoRepositoryPort historicoPort;
    private final AlpacaMarketDataPort alpacaPort;
    private final CatalogoClientPort catalogoPort;

    public ObtenerHistoricoUseCaseImpl(HistoricoRepositoryPort historicoPort, AlpacaMarketDataPort alpacaPort, CatalogoClientPort catalogoPort) {
        this.historicoPort = historicoPort;
        this.alpacaPort = alpacaPort;
        this.catalogoPort = catalogoPort;
    }

    @Override
    public List<HistoricoCotizacion> obtenerHistorico(String symbol, Instant from, Instant to, Intervalo intervalo) {
        if (!catalogoPort.validarSimbolo(symbol)) {
            throw new SimboloInvalidoException(symbol);
        }

        List<HistoricoCotizacion> cached = historicoPort.findBySymbolAndIntervaloAndTimestampBetween(symbol, intervalo, from, to);
        if (!cached.isEmpty()) {
            return cached;
        }

        List<HistoricoCotizacion> fresh = alpacaPort.obtenerHistorico(symbol, from, to, intervalo);
        if (!fresh.isEmpty()) {
            historicoPort.saveAll(fresh);
        }
        return fresh;
    }
}
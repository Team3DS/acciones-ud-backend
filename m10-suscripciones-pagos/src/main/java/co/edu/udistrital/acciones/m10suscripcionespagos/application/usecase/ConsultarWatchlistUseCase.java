package co.edu.udistrital.acciones.m10suscripcionespagos.application.usecase;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.Cotizacion;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.WatchlistItemResponse;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.PremiumRequeridoException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.CotizacionClientPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.WatchlistRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.service.EvaluadorPremium;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.WatchlistItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ConsultarWatchlistUseCase {

    private static final Logger log = LoggerFactory.getLogger(ConsultarWatchlistUseCase.class);

    private final WatchlistRepositoryPort repositorio;
    private final EvaluadorPremium evaluador;
    private final CotizacionClientPort cotizaciones;
    private final Clock clock;

    public ConsultarWatchlistUseCase(WatchlistRepositoryPort repositorio, EvaluadorPremium evaluador,
                                     CotizacionClientPort cotizaciones, Clock clock) {
        this.repositorio = repositorio;
        this.evaluador = evaluador;
        this.cotizaciones = cotizaciones;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<WatchlistItemResponse> ejecutar(Long userId) {
        exigirPremium(userId);
        List<WatchlistItem> items = repositorio.findByUserId(userId);
        if (items.isEmpty()) {
            return List.of();
        }
        Set<String> symbols = new LinkedHashSet<>();
        for (WatchlistItem item : items) {
            symbols.add(item.getSymbol());
        }
        Map<String, Cotizacion> precios = Map.of();
        boolean stale = false;
        try {
            precios = cotizaciones.obtenerCotizaciones(symbols);
        } catch (RuntimeException ex) {
            log.warn("No se obtuvieron las cotizaciones de la watchlist", ex);
            stale = true;
        }
        Map<String, Cotizacion> cotizacionesObtenidas = precios;
        boolean sinCotizaciones = stale;
        return items.stream()
                .map(item -> WatchlistItemResponse.desde(
                        item, cotizacionesObtenidas.get(item.getSymbol()), sinCotizaciones))
                .toList();
    }

    private void exigirPremium(Long userId) {
        if (!evaluador.evaluar(userId, Instant.now(clock)).premium()) {
            throw new PremiumRequeridoException();
        }
    }
}

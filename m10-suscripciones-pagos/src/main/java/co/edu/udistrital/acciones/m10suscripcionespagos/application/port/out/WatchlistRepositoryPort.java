package co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out;

import co.edu.udistrital.acciones.m10suscripcionespagos.domain.WatchlistItem;

import java.util.List;
import java.util.Optional;

public interface WatchlistRepositoryPort {

    List<WatchlistItem> findByUserId(Long userId);

    Optional<WatchlistItem> findByUserIdAndSymbol(Long userId, String symbol);

    WatchlistItem save(WatchlistItem item);

    void delete(WatchlistItem item);
}

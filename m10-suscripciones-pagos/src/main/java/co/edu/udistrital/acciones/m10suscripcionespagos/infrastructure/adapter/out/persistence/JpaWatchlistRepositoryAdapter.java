package co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.WatchlistRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.WatchlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
interface SpringDataWatchlistRepository extends JpaRepository<WatchlistItem, Long> {

    List<WatchlistItem> findByUserIdOrderByAgregadoEnAsc(Long userId);

    Optional<WatchlistItem> findByUserIdAndSymbol(Long userId, String symbol);
}

@Repository
public class JpaWatchlistRepositoryAdapter implements WatchlistRepositoryPort {

    private final SpringDataWatchlistRepository repositorio;

    public JpaWatchlistRepositoryAdapter(SpringDataWatchlistRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public List<WatchlistItem> findByUserId(Long userId) {
        return repositorio.findByUserIdOrderByAgregadoEnAsc(userId);
    }

    @Override
    public Optional<WatchlistItem> findByUserIdAndSymbol(Long userId, String symbol) {
        return repositorio.findByUserIdAndSymbol(userId, symbol);
    }

    @Override
    public WatchlistItem save(WatchlistItem item) {
        return repositorio.save(item);
    }

    @Override
    public void delete(WatchlistItem item) {
        repositorio.delete(item);
    }
}

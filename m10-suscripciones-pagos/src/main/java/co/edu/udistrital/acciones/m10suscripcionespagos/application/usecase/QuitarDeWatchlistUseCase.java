package co.edu.udistrital.acciones.m10suscripcionespagos.application.usecase;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.PremiumRequeridoException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.WatchlistRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.service.EvaluadorPremium;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.Simbolo;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.WatchlistItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class QuitarDeWatchlistUseCase {

    private final WatchlistRepositoryPort repositorio;
    private final EvaluadorPremium evaluador;
    private final Clock clock;

    public QuitarDeWatchlistUseCase(WatchlistRepositoryPort repositorio, EvaluadorPremium evaluador,
                                    Clock clock) {
        this.repositorio = repositorio;
        this.evaluador = evaluador;
        this.clock = clock;
    }

    @Transactional
    public void ejecutar(Long userId, String symbol) {
        exigirPremium(userId);
        Simbolo simbolo = Simbolo.desde(symbol);
        repositorio.findByUserIdAndSymbol(userId, simbolo.valor()).ifPresent(repositorio::delete);
    }

    private void exigirPremium(Long userId) {
        if (!evaluador.evaluar(userId, Instant.now(clock)).premium()) {
            throw new PremiumRequeridoException();
        }
    }
}

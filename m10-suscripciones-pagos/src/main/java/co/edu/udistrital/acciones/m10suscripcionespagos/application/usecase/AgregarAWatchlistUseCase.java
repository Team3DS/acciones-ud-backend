package co.edu.udistrital.acciones.m10suscripcionespagos.application.usecase;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.ResultadoAgregado;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.SolicitudWatchlist;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.PremiumRequeridoException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.SimboloNoDisponibleException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.CatalogoClientPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.WatchlistRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.service.EvaluadorPremium;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.Simbolo;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.WatchlistItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Service
public class AgregarAWatchlistUseCase {

    private final WatchlistRepositoryPort repositorio;
    private final EvaluadorPremium evaluador;
    private final CatalogoClientPort catalogo;
    private final Clock clock;

    public AgregarAWatchlistUseCase(WatchlistRepositoryPort repositorio, EvaluadorPremium evaluador,
                                    CatalogoClientPort catalogo, Clock clock) {
        this.repositorio = repositorio;
        this.evaluador = evaluador;
        this.catalogo = catalogo;
        this.clock = clock;
    }

    @Transactional
    public ResultadoAgregado ejecutar(Long userId, SolicitudWatchlist solicitud) {
        exigirPremium(userId);
        Simbolo simbolo = Simbolo.desde(solicitud.symbol());
        Optional<WatchlistItem> existente = repositorio.findByUserIdAndSymbol(userId, simbolo.valor());
        if (existente.isPresent()) {
            return new ResultadoAgregado(existente.get(), false);
        }
        if (!catalogo.esValido(simbolo)) {
            throw new SimboloNoDisponibleException(simbolo.valor());
        }
        WatchlistItem creado = repositorio.save(
                WatchlistItem.crear(userId, simbolo, Instant.now(clock)));
        return new ResultadoAgregado(creado, true);
    }

    private void exigirPremium(Long userId) {
        if (!evaluador.evaluar(userId, Instant.now(clock)).premium()) {
            throw new PremiumRequeridoException();
        }
    }
}

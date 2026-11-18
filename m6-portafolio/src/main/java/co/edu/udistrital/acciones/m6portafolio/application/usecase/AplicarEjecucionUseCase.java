package co.edu.udistrital.acciones.m6portafolio.application.usecase;

import co.edu.udistrital.acciones.m6portafolio.application.dto.Actor;
import co.edu.udistrital.acciones.m6portafolio.application.dto.EjecucionRequest;
import co.edu.udistrital.acciones.m6portafolio.application.dto.EjecucionResponse;
import co.edu.udistrital.acciones.m6portafolio.application.dto.PosicionesActualizadasPayload;
import co.edu.udistrital.acciones.m6portafolio.application.dto.RoutingKeys;
import co.edu.udistrital.acciones.m6portafolio.application.exception.ConflictoIdempotenciaException;
import co.edu.udistrital.acciones.m6portafolio.application.exception.RecursoNoEncontradoException;
import co.edu.udistrital.acciones.m6portafolio.application.port.out.MovimientoPosicionRepositoryPort;
import co.edu.udistrital.acciones.m6portafolio.application.port.out.OutboxPort;
import co.edu.udistrital.acciones.m6portafolio.application.port.out.PosicionRepositoryPort;
import co.edu.udistrital.acciones.m6portafolio.application.service.ValoracionService;
import co.edu.udistrital.acciones.m6portafolio.domain.LadoEjecucion;
import co.edu.udistrital.acciones.m6portafolio.domain.MovimientoPosicion;
import co.edu.udistrital.acciones.m6portafolio.domain.Posicion;
import co.edu.udistrital.acciones.m6portafolio.domain.Simbolos;
import co.edu.udistrital.acciones.m6portafolio.domain.excepcion.VentaNoDisponibleException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class AplicarEjecucionUseCase {

    public static final String PREFIJO_CLAVE = "EXE:";

    private final PosicionRepositoryPort posiciones;
    private final MovimientoPosicionRepositoryPort movimientos;
    private final ValoracionService valoracion;
    private final OutboxPort outbox;
    private final Clock clock;

    public AplicarEjecucionUseCase(PosicionRepositoryPort posiciones,
                                   MovimientoPosicionRepositoryPort movimientos,
                                   ValoracionService valoracion, OutboxPort outbox, Clock clock) {
        this.posiciones = posiciones;
        this.movimientos = movimientos;
        this.valoracion = valoracion;
        this.outbox = outbox;
        this.clock = clock;
    }

    @Transactional
    public EjecucionResponse ejecutar(EjecucionRequest peticion, Actor actor) {
        actor.verificarOperacion(peticion.userId());
        String symbol = Simbolos.normalizar(peticion.symbol());
        String clave = PREFIJO_CLAVE + peticion.executionId();
        if (movimientos.existsByIdempotencyKey(clave)) {
            return repetida(peticion.userId(), symbol);
        }
        if (peticion.side() == LadoEjecucion.SELL) {
            throw new VentaNoDisponibleException(
                    "la venta estara disponible cuando se implementen las reservas");
        }
        Instant ahora = Instant.now(clock);
        Instant ocurrido = peticion.executedAt() == null ? ahora : peticion.executedAt();
        posiciones.crearSiNoExiste(peticion.userId(), symbol, ahora);
        Posicion posicion = posiciones.findByUserIdAndSymbolForUpdate(peticion.userId(), symbol)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la posicion " + symbol + " del usuario " + peticion.userId()));
        if (movimientos.existsByIdempotencyKey(clave)) {
            return new EjecucionResponse(false, valoracion.valorar(posicion));
        }
        posicion.comprar(peticion.quantity(), peticion.price(), ocurrido);
        posiciones.save(posicion);
        movimientos.save(MovimientoPosicion.compra(posicion.getId(), peticion.quantity(), peticion.price(),
                peticion.executionId(), peticion.orderId(), clave, ocurrido));
        outbox.registrar(RoutingKeys.TIPO_AGREGADO_POSICION, String.valueOf(posicion.getId()),
                RoutingKeys.POSICIONES_ACTUALIZADAS,
                new PosicionesActualizadasPayload(peticion.orderId(), peticion.executionId(),
                        peticion.userId(), symbol, peticion.side().name(), peticion.quantity(),
                        peticion.price(), posicion.getCantidad(), posicion.getCostoPromedio(),
                        posicion.getPnlRealizado()));
        return new EjecucionResponse(true, valoracion.valorar(posicion));
    }

    private EjecucionResponse repetida(Long userId, String symbol) {
        return posiciones.findByUserIdAndSymbol(userId, symbol)
                .map(posicion -> new EjecucionResponse(false, valoracion.valorar(posicion)))
                .orElseThrow(() -> new ConflictoIdempotenciaException(
                        "La ejecucion ya fue aplicada y la posicion no esta disponible"));
    }
}

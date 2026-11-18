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
import co.edu.udistrital.acciones.m6portafolio.application.port.out.ReservaPosicionRepositoryPort;
import co.edu.udistrital.acciones.m6portafolio.application.service.ValoracionService;
import co.edu.udistrital.acciones.m6portafolio.domain.EstadoReservaPosicion;
import co.edu.udistrital.acciones.m6portafolio.domain.LadoEjecucion;
import co.edu.udistrital.acciones.m6portafolio.domain.MovimientoPosicion;
import co.edu.udistrital.acciones.m6portafolio.domain.Posicion;
import co.edu.udistrital.acciones.m6portafolio.domain.ReservaPosicion;
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
    private final ReservaPosicionRepositoryPort reservas;
    private final MovimientoPosicionRepositoryPort movimientos;
    private final ValoracionService valoracion;
    private final OutboxPort outbox;
    private final Clock clock;

    public AplicarEjecucionUseCase(PosicionRepositoryPort posiciones, ReservaPosicionRepositoryPort reservas,
                                   MovimientoPosicionRepositoryPort movimientos,
                                   ValoracionService valoracion, OutboxPort outbox, Clock clock) {
        this.posiciones = posiciones;
        this.reservas = reservas;
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
        Instant ahora = Instant.now(clock);
        Instant ocurrido = peticion.executedAt() == null ? ahora : peticion.executedAt();
        Posicion posicion = obtenerPosicionBloqueada(peticion, symbol, ahora);
        if (movimientos.existsByIdempotencyKey(clave)) {
            return new EjecucionResponse(false, valoracion.valorar(posicion));
        }
        aplicarMovimiento(peticion, posicion, ocurrido);
        posiciones.save(posicion);
        movimientos.save(crearMovimiento(peticion, posicion, clave, ocurrido));
        outbox.registrar(RoutingKeys.TIPO_AGREGADO_POSICION, String.valueOf(posicion.getId()),
                RoutingKeys.POSICIONES_ACTUALIZADAS,
                new PosicionesActualizadasPayload(peticion.orderId(), peticion.executionId(),
                        peticion.userId(), symbol, peticion.side().name(), peticion.quantity(),
                        peticion.price(), posicion.getCantidad(), posicion.getCostoPromedio(),
                        posicion.getPnlRealizado()));
        return new EjecucionResponse(true, valoracion.valorar(posicion));
    }

    private Posicion obtenerPosicionBloqueada(EjecucionRequest peticion, String symbol, Instant ahora) {
        if (peticion.side() == LadoEjecucion.BUY) {
            posiciones.crearSiNoExiste(peticion.userId(), symbol, ahora);
        }
        return posiciones.findByUserIdAndSymbolForUpdate(peticion.userId(), symbol)
                .orElseThrow(() -> posicionNoDisponible(peticion, symbol));
    }

    private RuntimeException posicionNoDisponible(EjecucionRequest peticion, String symbol) {
        String mensaje = "No existe la posicion " + symbol + " del usuario " + peticion.userId();
        if (peticion.side() == LadoEjecucion.SELL) {
            return new VentaNoDisponibleException(mensaje);
        }
        return new RecursoNoEncontradoException(mensaje);
    }

    private void aplicarMovimiento(EjecucionRequest peticion, Posicion posicion, Instant ocurrido) {
        if (peticion.side() == LadoEjecucion.BUY) {
            posicion.comprar(peticion.quantity(), peticion.price(), ocurrido);
            return;
        }
        ReservaPosicion reserva = reservas.findByOrderIdForUpdate(peticion.orderId()).orElse(null);
        if (reserva != null && reserva.getEstado() == EstadoReservaPosicion.PENDING) {
            if (!reserva.getPosicionId().equals(posicion.getId())) {
                throw new VentaNoDisponibleException("La reserva no corresponde a la posicion de la venta");
            }
            reserva.ejecutar(peticion.quantity(), ocurrido);
            posicion.venderReservado(peticion.quantity(), peticion.price(), ocurrido);
            reservas.save(reserva);
            return;
        }
        posicion.venderDisponible(peticion.quantity(), peticion.price(), ocurrido);
    }

    private MovimientoPosicion crearMovimiento(EjecucionRequest peticion, Posicion posicion, String clave,
                                               Instant ocurrido) {
        if (peticion.side() == LadoEjecucion.BUY) {
            return MovimientoPosicion.compra(posicion.getId(), peticion.quantity(), peticion.price(),
                    peticion.executionId(), peticion.orderId(), clave, ocurrido);
        }
        return MovimientoPosicion.venta(posicion.getId(), peticion.quantity(), peticion.price(),
                peticion.executionId(), peticion.orderId(), clave, ocurrido);
    }

    private EjecucionResponse repetida(Long userId, String symbol) {
        return posiciones.findByUserIdAndSymbol(userId, symbol)
                .map(posicion -> new EjecucionResponse(false, valoracion.valorar(posicion)))
                .orElseThrow(() -> new ConflictoIdempotenciaException(
                        "La ejecucion ya fue aplicada y la posicion no esta disponible"));
    }
}

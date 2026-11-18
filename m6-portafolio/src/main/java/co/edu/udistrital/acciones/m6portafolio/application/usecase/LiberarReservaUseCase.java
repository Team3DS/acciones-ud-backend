package co.edu.udistrital.acciones.m6portafolio.application.usecase;

import co.edu.udistrital.acciones.m6portafolio.application.dto.Actor;
import co.edu.udistrital.acciones.m6portafolio.application.dto.ReservaPosicionResponse;
import co.edu.udistrital.acciones.m6portafolio.application.exception.ConflictoEstadoReservaException;
import co.edu.udistrital.acciones.m6portafolio.application.exception.RecursoNoEncontradoException;
import co.edu.udistrital.acciones.m6portafolio.application.port.out.MovimientoPosicionRepositoryPort;
import co.edu.udistrital.acciones.m6portafolio.application.port.out.PosicionRepositoryPort;
import co.edu.udistrital.acciones.m6portafolio.application.port.out.ReservaPosicionRepositoryPort;
import co.edu.udistrital.acciones.m6portafolio.domain.EstadoReservaPosicion;
import co.edu.udistrital.acciones.m6portafolio.domain.MovimientoPosicion;
import co.edu.udistrital.acciones.m6portafolio.domain.Posicion;
import co.edu.udistrital.acciones.m6portafolio.domain.ReservaPosicion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

@Service
public class LiberarReservaUseCase {

    public static final String PREFIJO_MOVIMIENTO = "LIB:";

    private final PosicionRepositoryPort posiciones;
    private final ReservaPosicionRepositoryPort reservas;
    private final MovimientoPosicionRepositoryPort movimientos;
    private final Clock clock;

    public LiberarReservaUseCase(PosicionRepositoryPort posiciones, ReservaPosicionRepositoryPort reservas,
                                 MovimientoPosicionRepositoryPort movimientos, Clock clock) {
        this.posiciones = posiciones;
        this.reservas = reservas;
        this.movimientos = movimientos;
        this.clock = clock;
    }

    @Transactional
    public ReservaPosicionResponse ejecutar(Long reservaId, String motivo, Actor actor) {
        ReservaPosicion referencia = reservas.findById(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la reserva " + reservaId));
        Posicion posicion = posiciones.findByIdForUpdate(referencia.getPosicionId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la posicion de la reserva " + reservaId));
        actor.verificarOperacion(posicion.getUserId());

        ReservaPosicion reserva = reservas.findByIdForUpdate(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la reserva " + reservaId));
        if (reserva.getEstado() == EstadoReservaPosicion.CONFIRMED) {
            throw new ConflictoEstadoReservaException("La reserva " + reservaId + " ya fue confirmada");
        }
        if (reserva.getEstado() == EstadoReservaPosicion.RELEASED) {
            return respuesta(posicion, reserva);
        }

        Instant ahora = Instant.now(clock);
        BigDecimal pendiente = reserva.liberar(motivo, ahora);
        if (pendiente.signum() > 0) {
            posicion.liberarReserva(pendiente, ahora);
            posiciones.save(posicion);
            movimientos.save(MovimientoPosicion.liberacion(posicion.getId(), pendiente,
                    posicion.getCostoPromedio(), reserva.getOrderId(),
                    PREFIJO_MOVIMIENTO + reserva.getId(), ahora));
        }
        reservas.save(reserva);
        return respuesta(posicion, reserva);
    }

    private static ReservaPosicionResponse respuesta(Posicion posicion, ReservaPosicion reserva) {
        return new ReservaPosicionResponse(reserva.getId(), reserva.getOrderId(), posicion.getUserId(),
                posicion.getSymbol(), reserva.getCantidad(), reserva.getCantidadEjecutada(),
                reserva.getEstado().name(), reserva.getMotivoLiberacion(), reserva.getCreadoEn(),
                reserva.getConfirmadoEn(), reserva.getLiberadoEn());
    }
}

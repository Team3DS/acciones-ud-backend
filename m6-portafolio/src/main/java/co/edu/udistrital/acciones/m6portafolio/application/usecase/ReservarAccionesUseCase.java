package co.edu.udistrital.acciones.m6portafolio.application.usecase;

import co.edu.udistrital.acciones.m6portafolio.application.dto.Actor;
import co.edu.udistrital.acciones.m6portafolio.application.dto.ReservaPosicionResponse;
import co.edu.udistrital.acciones.m6portafolio.application.dto.ReservaRequest;
import co.edu.udistrital.acciones.m6portafolio.application.exception.ConflictoIdempotenciaException;
import co.edu.udistrital.acciones.m6portafolio.application.port.out.MovimientoPosicionRepositoryPort;
import co.edu.udistrital.acciones.m6portafolio.application.port.out.PosicionRepositoryPort;
import co.edu.udistrital.acciones.m6portafolio.application.port.out.ReservaPosicionRepositoryPort;
import co.edu.udistrital.acciones.m6portafolio.domain.Cantidades;
import co.edu.udistrital.acciones.m6portafolio.domain.MovimientoPosicion;
import co.edu.udistrital.acciones.m6portafolio.domain.Posicion;
import co.edu.udistrital.acciones.m6portafolio.domain.ReservaPosicion;
import co.edu.udistrital.acciones.m6portafolio.domain.Simbolos;
import co.edu.udistrital.acciones.m6portafolio.domain.excepcion.ReservaNoDisponibleException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Service
public class ReservarAccionesUseCase {

    public static final String PREFIJO_CLAVE = "RSV:";
    public static final String PREFIJO_MOVIMIENTO = "RES:";

    private final PosicionRepositoryPort posiciones;
    private final ReservaPosicionRepositoryPort reservas;
    private final MovimientoPosicionRepositoryPort movimientos;
    private final Clock clock;

    public ReservarAccionesUseCase(PosicionRepositoryPort posiciones, ReservaPosicionRepositoryPort reservas,
                                   MovimientoPosicionRepositoryPort movimientos, Clock clock) {
        this.posiciones = posiciones;
        this.reservas = reservas;
        this.movimientos = movimientos;
        this.clock = clock;
    }

    @Transactional
    public ReservaPosicionResponse ejecutar(ReservaRequest peticion, String clavePeticion, Actor actor) {
        actor.verificarOperacion(peticion.userId());
        String symbol = Simbolos.normalizar(peticion.symbol());
        BigDecimal cantidad = Cantidades.validarPositiva(peticion.quantity());
        String clave = PREFIJO_CLAVE + clavePeticion;

        Optional<ReservaPosicion> previa = reservas.findByIdempotencyKey(clave);
        if (previa.isPresent()) {
            return repetir(previa.get(), peticion, symbol, cantidad);
        }

        Posicion posicion = posiciones.findByUserIdAndSymbolForUpdate(peticion.userId(), symbol)
                .orElseThrow(() -> new ReservaNoDisponibleException(
                        "No existe la posicion " + symbol + " del usuario " + peticion.userId()));

        Optional<ReservaPosicion> trasBloqueo = reservas.findByIdempotencyKey(clave);
        if (trasBloqueo.isPresent()) {
            return repetir(trasBloqueo.get(), peticion, symbol, cantidad);
        }
        if (reservas.findByOrderId(peticion.orderId()).isPresent()) {
            throw new ConflictoIdempotenciaException(
                    "Ya existe una reserva para la orden " + peticion.orderId());
        }

        Instant ahora = Instant.now(clock);
        posicion.reservar(cantidad, ahora);
        posiciones.save(posicion);
        ReservaPosicion reserva = reservas.save(ReservaPosicion.crear(posicion.getId(), peticion.orderId(),
                cantidad, clave, ahora));
        movimientos.save(MovimientoPosicion.reserva(posicion.getId(), cantidad, posicion.getCostoPromedio(),
                peticion.orderId(), PREFIJO_MOVIMIENTO + reserva.getId(), ahora));
        return respuesta(posicion, reserva);
    }

    private ReservaPosicionResponse repetir(ReservaPosicion reserva, ReservaRequest peticion, String symbol,
                                            BigDecimal cantidad) {
        Posicion posicion = posiciones.findById(reserva.getPosicionId())
                .orElseThrow(() -> new ConflictoIdempotenciaException(
                        "La clave de idempotencia ya fue usada con otros datos"));
        boolean mismosDatos = posicion.getUserId().equals(peticion.userId())
                && posicion.getSymbol().equals(symbol)
                && reserva.getOrderId().equals(peticion.orderId())
                && reserva.getCantidad().compareTo(cantidad) == 0;
        if (!mismosDatos) {
            throw new ConflictoIdempotenciaException("La clave de idempotencia ya fue usada con otros datos");
        }
        return respuesta(posicion, reserva);
    }

    private static ReservaPosicionResponse respuesta(Posicion posicion, ReservaPosicion reserva) {
        return new ReservaPosicionResponse(reserva.getId(), reserva.getOrderId(), posicion.getUserId(),
                posicion.getSymbol(), reserva.getCantidad(), reserva.getCantidadEjecutada(),
                reserva.getEstado().name(), reserva.getMotivoLiberacion(), reserva.getCreadoEn(),
                reserva.getConfirmadoEn(), reserva.getLiberadoEn());
    }
}

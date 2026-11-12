package co.edu.udistrital.acciones.m2cuentasfondos.application.usecase;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.Actor;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.FondosLiberadosPayload;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.LiberarReservaRequest;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.ReservaResponse;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.RoutingKeys;
import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.AccesoDenegadoException;
import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.RecursoNoEncontradoException;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.CuentaRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.OutboxPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.ReservaRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.service.LibroMovimientos;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Cuenta;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.EstadoReserva;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Reserva;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.TipoMovimiento;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.excepcion.TransicionReservaInvalidaException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class LiberarReservaUseCase {

    public static final String PREFIJO_MOVIMIENTO = "LIB:";
    public static final String MONEDA = "USD";

    private final CuentaRepositoryPort cuentas;
    private final ReservaRepositoryPort reservas;
    private final LibroMovimientos libro;
    private final OutboxPort outbox;
    private final Clock clock;

    public LiberarReservaUseCase(CuentaRepositoryPort cuentas, ReservaRepositoryPort reservas,
                                 LibroMovimientos libro, OutboxPort outbox, Clock clock) {
        this.cuentas = cuentas;
        this.reservas = reservas;
        this.libro = libro;
        this.outbox = outbox;
        this.clock = clock;
    }

    @Transactional
    public ReservaResponse ejecutar(Long reservaId, LiberarReservaRequest request, Actor actor) {
        Reserva referencia = reservas.findById(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la reserva " + reservaId));
        Cuenta cuenta = cuentas.findByIdForUpdate(referencia.getCuentaId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la cuenta de la reserva " + reservaId));
        if (!actor.puedeOperar(cuenta.getUserId())) {
            throw new AccesoDenegadoException("Solo el dueño puede liberar esta reserva");
        }
        Reserva reserva = reservas.findByIdForUpdate(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la reserva " + reservaId));

        if (reserva.getEstado() == EstadoReserva.RELEASED || reserva.getEstado() == EstadoReserva.EXPIRED) {
            return respuesta(cuenta, reserva);
        }
        if (reserva.getEstado() != EstadoReserva.PENDING) {
            throw new TransicionReservaInvalidaException(
                    "No se puede liberar una reserva en estado " + reserva.getEstado());
        }

        Instant ahora = Instant.now(clock);
        cuenta.liberarReserva(reserva.getMonto(), ahora);
        cuentas.save(cuenta);

        reserva.liberar(request.reason(), ahora);
        reservas.save(reserva);

        libro.registrar(cuenta, TipoMovimiento.LIBERACION, reserva.getMonto(), TipoMovimiento.LIBERACION.name(),
                PREFIJO_MOVIMIENTO + reserva.getId(),
                "Liberación de la reserva " + reserva.getId() + ": " + request.reason());

        outbox.registrar("Reserva", reserva.getId().toString(), RoutingKeys.FONDOS_LIBERADOS,
                new FondosLiberadosPayload(reserva.getId(), request.reason()));

        return respuesta(cuenta, reserva);
    }

    private ReservaResponse respuesta(Cuenta cuenta, Reserva reserva) {
        return new ReservaResponse(reserva.getId(), reserva.getOrderId(), cuenta.getUserId(), reserva.getMonto(),
                reserva.getEstado().name(), cuenta.getSaldoDisponible(), cuenta.getSaldoReservado(),
                cuenta.getSaldoContable(), MONEDA, reserva.getCreadoEn());
    }
}

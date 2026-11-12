package co.edu.udistrital.acciones.m2cuentasfondos.application.usecase;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.Actor;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.ConfirmarReservaRequest;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.FondosConfirmadosPayload;
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
import co.edu.udistrital.acciones.m2cuentasfondos.domain.MonetaryUtil;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Reserva;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.TipoMovimiento;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.excepcion.TransicionReservaInvalidaException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

@Service
public class ConfirmarReservaUseCase {

    public static final String PREFIJO_DEBITO = "CNF:";
    public static final String PREFIJO_LIBERACION = "CNF-LIB:";
    public static final String MONEDA = "USD";

    private final CuentaRepositoryPort cuentas;
    private final ReservaRepositoryPort reservas;
    private final LibroMovimientos libro;
    private final OutboxPort outbox;
    private final Clock clock;

    public ConfirmarReservaUseCase(CuentaRepositoryPort cuentas, ReservaRepositoryPort reservas,
                                   LibroMovimientos libro, OutboxPort outbox, Clock clock) {
        this.cuentas = cuentas;
        this.reservas = reservas;
        this.libro = libro;
        this.outbox = outbox;
        this.clock = clock;
    }

    @Transactional
    public ReservaResponse ejecutar(Long reservaId, ConfirmarReservaRequest request, Actor actor) {
        BigDecimal ejecutado = MonetaryUtil.validarPositivo(request.amount());
        Reserva referencia = reservas.findById(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la reserva " + reservaId));
        Cuenta cuenta = cuentas.findByIdForUpdate(referencia.getCuentaId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la cuenta de la reserva " + reservaId));
        if (!actor.puedeOperar(cuenta.getUserId())) {
            throw new AccesoDenegadoException("Solo el dueño puede confirmar esta reserva");
        }
        Reserva reserva = reservas.findByIdForUpdate(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la reserva " + reservaId));

        if (reserva.getEstado() == EstadoReserva.CONFIRMED) {
            return repetir(cuenta, reserva, request.executionId());
        }
        if (reserva.getEstado() != EstadoReserva.PENDING) {
            throw new TransicionReservaInvalidaException(
                    "No se puede confirmar una reserva en estado " + reserva.getEstado());
        }

        Instant ahora = Instant.now(clock);
        BigDecimal reservado = reserva.getMonto();
        cuenta.confirmarReserva(reservado, ejecutado, ahora);
        cuentas.save(cuenta);

        reserva.confirmar(request.executionId(), ahora);
        reservas.save(reserva);

        libro.registrar(cuenta, TipoMovimiento.DEBITO_COMPRA, ejecutado, TipoMovimiento.DEBITO_COMPRA.name(),
                PREFIJO_DEBITO + reserva.getId(),
                "Confirmación de la reserva " + reserva.getId() + " para la orden " + reserva.getOrderId());

        if (reservado.compareTo(ejecutado) > 0) {
            BigDecimal sobrante = reservado.subtract(ejecutado);
            libro.registrar(cuenta, TipoMovimiento.LIBERACION, sobrante, TipoMovimiento.LIBERACION.name(),
                    PREFIJO_LIBERACION + reserva.getId(),
                    "Liberación del sobrante de la reserva " + reserva.getId());
        }

        outbox.registrar("Reserva", reserva.getId().toString(), RoutingKeys.FONDOS_CONFIRMADOS,
                new FondosConfirmadosPayload(reserva.getId(), reserva.getExecutionId(), ejecutado));

        return respuesta(cuenta, reserva);
    }

    private ReservaResponse repetir(Cuenta cuenta, Reserva reserva, String executionId) {
        if (!Objects.equals(reserva.getExecutionId(), executionId)) {
            throw new TransicionReservaInvalidaException(
                    "La reserva " + reserva.getId() + " ya fue confirmada con otra ejecución");
        }
        return respuesta(cuenta, reserva);
    }

    private ReservaResponse respuesta(Cuenta cuenta, Reserva reserva) {
        return new ReservaResponse(reserva.getId(), reserva.getOrderId(), cuenta.getUserId(), reserva.getMonto(),
                reserva.getEstado().name(), cuenta.getSaldoDisponible(), cuenta.getSaldoReservado(),
                cuenta.getSaldoContable(), MONEDA, reserva.getCreadoEn());
    }
}

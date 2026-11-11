package co.edu.udistrital.acciones.m2cuentasfondos.application.usecase;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.Actor;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.FondosReservadosPayload;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.ReservaRequest;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.ReservaResponse;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.RoutingKeys;
import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.AccesoDenegadoException;
import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.ConflictoIdempotenciaException;
import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.ConflictoReservaException;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.CuentaRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.OutboxPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.ReservaRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.service.LibroMovimientos;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Cuenta;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.MonetaryUtil;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Reserva;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.TipoMovimiento;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.excepcion.FondosInsuficientesException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Service
public class ReservarFondosUseCase {

    public static final String PREFIJO_CLAVE = "RES:";
    public static final String PREFIJO_MOVIMIENTO = "RES:";
    public static final String MONEDA = "USD";

    private final CuentaRepositoryPort cuentas;
    private final ReservaRepositoryPort reservas;
    private final LibroMovimientos libro;
    private final OutboxPort outbox;
    private final Clock clock;

    public ReservarFondosUseCase(CuentaRepositoryPort cuentas, ReservaRepositoryPort reservas, LibroMovimientos libro,
                                 OutboxPort outbox, Clock clock) {
        this.cuentas = cuentas;
        this.reservas = reservas;
        this.libro = libro;
        this.outbox = outbox;
        this.clock = clock;
    }

    @Transactional
    public ReservaResponse ejecutar(ReservaRequest request, String idempotencyKey, Actor actor) {
        verificarPropietario(request.userId(), actor);
        BigDecimal monto = MonetaryUtil.validarPositivo(request.amount());
        String clave = PREFIJO_CLAVE + idempotencyKey;
        Instant ahora = Instant.now(clock);

        Optional<Reserva> existente = reservas.findByIdempotencyKey(clave);
        if (existente.isPresent()) {
            return repetir(existente.get(), request, monto);
        }

        Cuenta cuenta = cuentas.findByUserIdForUpdate(request.userId())
                .orElseThrow(() -> new FondosInsuficientesException(
                        "El usuario " + request.userId() + " no tiene una cuenta con fondos disponibles"));

        Optional<Reserva> trasBloqueo = reservas.findByIdempotencyKey(clave);
        if (trasBloqueo.isPresent()) {
            return repetir(trasBloqueo.get(), request, monto);
        }

        if (reservas.findByOrderId(request.orderId()).isPresent()) {
            throw new ConflictoReservaException("Ya existe una reserva para la orden " + request.orderId());
        }

        cuenta.reservar(monto, ahora);
        cuentas.save(cuenta);

        Reserva reserva = reservas.save(Reserva.crear(cuenta.getId(), request.orderId(), monto, clave, ahora));

        libro.registrar(cuenta, TipoMovimiento.RESERVA, monto, TipoMovimiento.RESERVA.name(),
                PREFIJO_MOVIMIENTO + reserva.getId(), "Reserva de fondos para la orden " + request.orderId());

        outbox.registrar("Reserva", reserva.getId().toString(), RoutingKeys.FONDOS_RESERVADOS,
                new FondosReservadosPayload(reserva.getId(), reserva.getOrderId(), cuenta.getUserId(),
                        reserva.getMonto()));

        return respuesta(cuenta, reserva);
    }

    private ReservaResponse repetir(Reserva reserva, ReservaRequest request, BigDecimal monto) {
        Cuenta cuenta = cuentas.findById(reserva.getCuentaId())
                .orElseThrow(() -> new ConflictoIdempotenciaException(
                        "La clave de idempotencia ya fue usada con otros datos"));
        boolean mismosDatos = cuenta.getUserId().equals(request.userId())
                && reserva.getOrderId().equals(request.orderId())
                && reserva.getMonto().compareTo(monto) == 0;
        if (!mismosDatos) {
            throw new ConflictoIdempotenciaException("La clave de idempotencia ya fue usada con otros datos");
        }
        return respuesta(cuenta, reserva);
    }

    private ReservaResponse respuesta(Cuenta cuenta, Reserva reserva) {
        return new ReservaResponse(reserva.getId(), reserva.getOrderId(), cuenta.getUserId(), reserva.getMonto(),
                reserva.getEstado().name(), cuenta.getSaldoDisponible(), cuenta.getSaldoReservado(),
                cuenta.getSaldoContable(), MONEDA, reserva.getCreadoEn());
    }

    private void verificarPropietario(Long userId, Actor actor) {
        if (!actor.puedeOperar(userId)) {
            throw new AccesoDenegadoException("Solo el dueño puede reservar fondos de su cuenta");
        }
    }
}

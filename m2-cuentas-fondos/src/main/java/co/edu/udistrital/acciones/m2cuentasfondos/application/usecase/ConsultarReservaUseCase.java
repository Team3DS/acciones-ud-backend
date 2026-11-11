package co.edu.udistrital.acciones.m2cuentasfondos.application.usecase;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.Actor;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.ReservaResponse;
import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.AccesoDenegadoException;
import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.RecursoNoEncontradoException;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.CuentaRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.ReservaRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Cuenta;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Reserva;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultarReservaUseCase {

    public static final String MONEDA = "USD";

    private final CuentaRepositoryPort cuentas;
    private final ReservaRepositoryPort reservas;

    public ConsultarReservaUseCase(CuentaRepositoryPort cuentas, ReservaRepositoryPort reservas) {
        this.cuentas = cuentas;
        this.reservas = reservas;
    }

    @Transactional(readOnly = true)
    public ReservaResponse ejecutar(Long reservaId, Actor actor) {
        Reserva reserva = reservas.findById(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la reserva " + reservaId));
        Cuenta cuenta = cuentas.findById(reserva.getCuentaId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la cuenta de la reserva " + reservaId));
        if (!actor.puedeVer(cuenta.getUserId())) {
            throw new AccesoDenegadoException("No tiene permiso para consultar esta reserva");
        }
        return new ReservaResponse(reserva.getId(), reserva.getOrderId(), cuenta.getUserId(), reserva.getMonto(),
                reserva.getEstado().name(), cuenta.getSaldoDisponible(), cuenta.getSaldoReservado(),
                cuenta.getSaldoContable(), MONEDA, reserva.getCreadoEn());
    }
}

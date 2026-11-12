package co.edu.udistrital.acciones.m2cuentasfondos.application.usecase;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.Actor;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.RetiroRequest;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.RetiroResponse;
import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.AccesoDenegadoException;
import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.ConflictoIdempotenciaException;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.CuentaRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.MovimientoRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.service.LibroMovimientos;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Cuenta;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.MonetaryUtil;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Movimiento;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.TipoMovimiento;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.excepcion.FondosInsuficientesException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Service
public class SolicitarRetiroUseCase {

    public static final String PREFIJO_CLAVE = "RET:";
    public static final String MONEDA = "USD";

    private final CuentaRepositoryPort cuentas;
    private final MovimientoRepositoryPort movimientos;
    private final LibroMovimientos libro;
    private final Clock clock;

    public SolicitarRetiroUseCase(CuentaRepositoryPort cuentas, MovimientoRepositoryPort movimientos,
                                  LibroMovimientos libro, Clock clock) {
        this.cuentas = cuentas;
        this.movimientos = movimientos;
        this.libro = libro;
        this.clock = clock;
    }

    @Transactional
    public RetiroResponse ejecutar(RetiroRequest request, String idempotencyKey, Actor actor) {
        verificarPropietario(request.userId(), actor);
        BigDecimal monto = MonetaryUtil.validarPositivo(request.amount());
        String clave = PREFIJO_CLAVE + idempotencyKey;
        Instant ahora = Instant.now(clock);

        Optional<Movimiento> existente = movimientos.findByIdempotencyKey(clave);
        if (existente.isPresent()) {
            return repetir(existente.get(), request.userId(), monto);
        }

        Cuenta cuenta = cuentas.findByUserIdForUpdate(request.userId())
                .orElseThrow(() -> new FondosInsuficientesException(
                        "El usuario " + request.userId() + " no tiene una cuenta con fondos disponibles"));

        Optional<Movimiento> trasBloqueo = movimientos.findByIdempotencyKey(clave);
        if (trasBloqueo.isPresent()) {
            return repetir(trasBloqueo.get(), request.userId(), monto);
        }

        cuenta.cargar(monto, ahora);
        cuentas.save(cuenta);
        Movimiento movimiento = libro.registrar(cuenta, TipoMovimiento.RETIRO, monto,
                TipoMovimiento.RETIRO.name(), clave, request.description());
        return respuesta(cuenta, movimiento);
    }

    private RetiroResponse repetir(Movimiento movimiento, Long userId, BigDecimal monto) {
        Cuenta cuenta = cuentas.findByUserId(userId)
                .orElseThrow(() -> new ConflictoIdempotenciaException(
                        "La clave de idempotencia ya fue usada con otros datos"));
        boolean mismosDatos = cuenta.getId().equals(movimiento.getCuentaId())
                && movimiento.getTipo() == TipoMovimiento.RETIRO
                && movimiento.getMonto().compareTo(monto) == 0;
        if (!mismosDatos) {
            throw new ConflictoIdempotenciaException("La clave de idempotencia ya fue usada con otros datos");
        }
        return respuesta(cuenta, movimiento);
    }

    private RetiroResponse respuesta(Cuenta cuenta, Movimiento movimiento) {
        return new RetiroResponse(movimiento.getId(), cuenta.getUserId(), movimiento.getMonto(),
                cuenta.getSaldoDisponible(), cuenta.getSaldoReservado(), cuenta.getSaldoContable(),
                MONEDA, movimiento.getCreadoEn());
    }

    private void verificarPropietario(Long userId, Actor actor) {
        if (!actor.puedeOperar(userId)) {
            throw new AccesoDenegadoException("Solo el dueño puede retirar de su cuenta");
        }
    }
}

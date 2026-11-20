package co.edu.udistrital.acciones.m2cuentasfondos.application.usecase;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.Actor;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.DepositoRequest;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.DepositoResponse;
import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.AccesoDenegadoException;
import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.ConflictoIdempotenciaException;
import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.RecursoNoEncontradoException;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.CuentaRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.MovimientoRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.service.LibroMovimientos;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Cuenta;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.MonetaryUtil;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Movimiento;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.TipoMovimiento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Service
public class RegistrarDepositoUseCase {

    public static final String PREFIJO_CLAVE = "DEP:";
    public static final String MONEDA = "USD";
    public static final BigDecimal LIMITE_MAXIMO_DEPOSITO = new BigDecimal("1000000.00");

    private final CuentaRepositoryPort cuentas;
    private final MovimientoRepositoryPort movimientos;
    private final LibroMovimientos libro;
    private final Clock clock;

    public RegistrarDepositoUseCase(CuentaRepositoryPort cuentas, MovimientoRepositoryPort movimientos,
                                    LibroMovimientos libro, Clock clock) {
        this.cuentas = cuentas;
        this.movimientos = movimientos;
        this.libro = libro;
        this.clock = clock;
    }

    @Transactional
    public DepositoResponse ejecutar(DepositoRequest request, String idempotencyKey, Actor actor) {
        verificarAutorizacion(request.userId(), actor);
        BigDecimal monto = MonetaryUtil.validarPositivo(request.amount());
        if (monto.compareTo(LIMITE_MAXIMO_DEPOSITO) > 0) {
            throw new IllegalArgumentException("El monto del depósito supera el límite permitido de USD " + LIMITE_MAXIMO_DEPOSITO);
        }
        String clave = PREFIJO_CLAVE + idempotencyKey;
        Instant ahora = Instant.now(clock);

        Optional<Movimiento> existente = movimientos.findByIdempotencyKey(clave);
        if (existente.isPresent()) {
            return repetir(existente.get(), request.userId(), monto);
        }

        cuentas.crearSiNoExiste(request.userId(), ahora);
        Cuenta cuenta = cuentas.findByUserIdForUpdate(request.userId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la cuenta del usuario " + request.userId()));

        Optional<Movimiento> trasBloqueo = movimientos.findByIdempotencyKey(clave);
        if (trasBloqueo.isPresent()) {
            return repetir(trasBloqueo.get(), request.userId(), monto);
        }

        cuenta.abonar(monto, ahora);
        cuentas.save(cuenta);
        Movimiento movimiento = libro.registrar(cuenta, TipoMovimiento.DEPOSITO, monto,
                TipoMovimiento.DEPOSITO.name(), clave, request.description());
        return respuesta(cuenta, movimiento);
    }

    private DepositoResponse repetir(Movimiento movimiento, Long userId, BigDecimal monto) {
        Cuenta cuenta = cuentas.findByUserId(userId)
                .orElseThrow(() -> new ConflictoIdempotenciaException(
                        "La clave de idempotencia ya fue usada con otros datos"));
        boolean mismosDatos = cuenta.getId().equals(movimiento.getCuentaId())
                && movimiento.getTipo() == TipoMovimiento.DEPOSITO
                && movimiento.getMonto().compareTo(monto) == 0;
        if (!mismosDatos) {
            throw new ConflictoIdempotenciaException("La clave de idempotencia ya fue usada con otros datos");
        }
        return respuesta(cuenta, movimiento);
    }

    private DepositoResponse respuesta(Cuenta cuenta, Movimiento movimiento) {
        return new DepositoResponse(movimiento.getId(), cuenta.getUserId(), movimiento.getMonto(),
                cuenta.getSaldoDisponible(), cuenta.getSaldoReservado(), cuenta.getSaldoContable(),
                MONEDA, movimiento.getCreadoEn());
    }

    private void verificarAutorizacion(Long userId, Actor actor) {
        if (!actor.puedeOperar(userId) && !actor.administrador()) {
            throw new AccesoDenegadoException("No está autorizado para realizar depósitos en esta cuenta");
        }
    }
}

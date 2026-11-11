package co.edu.udistrital.acciones.m2cuentasfondos.application.service;

import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.MovimientoRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Cuenta;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Movimiento;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.TipoMovimiento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

@Component
public class LibroMovimientos {

    private final MovimientoRepositoryPort movimientos;
    private final Clock clock;

    public LibroMovimientos(MovimientoRepositoryPort movimientos, Clock clock) {
        this.movimientos = movimientos;
        this.clock = clock;
    }

    public Movimiento registrar(Cuenta cuenta, TipoMovimiento tipo, BigDecimal monto, String referencia,
                                String idempotencyKey, String descripcion) {
        Movimiento movimiento = Movimiento.crear(cuenta.getId(), tipo, monto, cuenta.getSaldoDisponible(),
                referencia, idempotencyKey, descripcion, Instant.now(clock));
        return movimientos.save(movimiento);
    }
}

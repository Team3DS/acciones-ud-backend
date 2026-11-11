package co.edu.udistrital.acciones.m2cuentasfondos.application.service;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.MovimientoCreadoPayload;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.RoutingKeys;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.MovimientoRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.OutboxPort;
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
    private final OutboxPort outbox;
    private final Clock clock;

    public LibroMovimientos(MovimientoRepositoryPort movimientos, OutboxPort outbox, Clock clock) {
        this.movimientos = movimientos;
        this.outbox = outbox;
        this.clock = clock;
    }

    public Movimiento registrar(Cuenta cuenta, TipoMovimiento tipo, BigDecimal monto, String referencia,
                                String idempotencyKey, String descripcion) {
        Movimiento movimiento = Movimiento.crear(cuenta.getId(), tipo, monto, cuenta.getSaldoDisponible(),
                referencia, idempotencyKey, descripcion, Instant.now(clock));
        Movimiento guardado = movimientos.save(movimiento);
        outbox.registrar("Movimiento", guardado.getId().toString(), RoutingKeys.MOVIMIENTO_CREADO,
                new MovimientoCreadoPayload(guardado.getId(), cuenta.getUserId(), guardado.getTipo().name(),
                        guardado.getMonto(), guardado.getSaldoPosterior()));
        return guardado;
    }
}

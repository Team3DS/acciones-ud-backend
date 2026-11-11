package co.edu.udistrital.acciones.m2cuentasfondos.application.port.out;

import co.edu.udistrital.acciones.m2cuentasfondos.domain.Movimiento;

import java.util.Optional;

public interface MovimientoRepositoryPort {

    Movimiento save(Movimiento movimiento);

    Optional<Movimiento> findByIdempotencyKey(String idempotencyKey);
}

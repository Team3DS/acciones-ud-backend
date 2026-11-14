package co.edu.udistrital.acciones.m2cuentasfondos.application.port.out;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.FiltroMovimientos;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Movimiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface MovimientoRepositoryPort {

    Movimiento save(Movimiento movimiento);

    Optional<Movimiento> findByIdempotencyKey(String idempotencyKey);

    Page<Movimiento> buscar(Long cuentaId, FiltroMovimientos filtro, Pageable pageable);
}

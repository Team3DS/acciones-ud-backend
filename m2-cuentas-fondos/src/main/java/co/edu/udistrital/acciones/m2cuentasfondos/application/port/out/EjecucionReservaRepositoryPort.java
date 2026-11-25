package co.edu.udistrital.acciones.m2cuentasfondos.application.port.out;

import co.edu.udistrital.acciones.m2cuentasfondos.domain.EjecucionReserva;
import java.util.Optional;

public interface EjecucionReservaRepositoryPort {
    Optional<EjecucionReserva> findByReservaIdAndExecutionId(Long reservaId, String executionId);
    EjecucionReserva save(EjecucionReserva ejecucion);
}

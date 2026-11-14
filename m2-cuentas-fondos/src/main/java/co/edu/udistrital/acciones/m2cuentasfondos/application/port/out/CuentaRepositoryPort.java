package co.edu.udistrital.acciones.m2cuentasfondos.application.port.out;

import co.edu.udistrital.acciones.m2cuentasfondos.domain.Cuenta;

import java.time.Instant;
import java.util.Optional;

public interface CuentaRepositoryPort {

    Optional<Cuenta> findById(Long id);

    Optional<Cuenta> findByIdForUpdate(Long id);

    Optional<Cuenta> findByUserId(Long userId);

    Optional<Cuenta> findByUserIdForUpdate(Long userId);

    void crearSiNoExiste(Long userId, Instant ahora);

    Cuenta save(Cuenta cuenta);
}

package co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.CuentaRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Cuenta;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
interface SpringDataCuentaRepository extends JpaRepository<Cuenta, Long> {

    Optional<Cuenta> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cuenta c where c.userId = :userId")
    Optional<Cuenta> findByUserIdForUpdate(@Param("userId") Long userId);

    @Modifying
    @Query(value = """
            INSERT INTO cuenta (user_id, saldo_contable, saldo_disponible, saldo_reservado, estado, creado_en, actualizado_en)
            VALUES (:userId, 0, 0, 0, 'ACTIVA', :ahora, :ahora)
            ON CONFLICT (user_id) DO NOTHING
            """, nativeQuery = true)
    void crearSiNoExiste(@Param("userId") Long userId, @Param("ahora") Instant ahora);
}

@Repository
public class JpaCuentaRepositoryAdapter implements CuentaRepositoryPort {

    private final SpringDataCuentaRepository repositorio;

    public JpaCuentaRepositoryAdapter(SpringDataCuentaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public Optional<Cuenta> findById(Long id) {
        return repositorio.findById(id);
    }

    @Override
    public Optional<Cuenta> findByUserId(Long userId) {
        return repositorio.findByUserId(userId);
    }

    @Override
    public Optional<Cuenta> findByUserIdForUpdate(Long userId) {
        return repositorio.findByUserIdForUpdate(userId);
    }

    @Override
    public void crearSiNoExiste(Long userId, Instant ahora) {
        repositorio.crearSiNoExiste(userId, ahora);
    }

    @Override
    public Cuenta save(Cuenta cuenta) {
        return repositorio.save(cuenta);
    }
}

package co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.ReservaRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Reserva;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
interface SpringDataReservaRepository extends JpaRepository<Reserva, Long> {

    Optional<Reserva> findByOrderId(String orderId);

    Optional<Reserva> findByIdempotencyKey(String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reserva r where r.id = :id")
    Optional<Reserva> findByIdForUpdate(@Param("id") Long id);
}

@Repository
public class JpaReservaRepositoryAdapter implements ReservaRepositoryPort {

    private final SpringDataReservaRepository repositorio;

    public JpaReservaRepositoryAdapter(SpringDataReservaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public Reserva save(Reserva reserva) {
        return repositorio.save(reserva);
    }

    @Override
    public Optional<Reserva> findById(Long id) {
        return repositorio.findById(id);
    }

    @Override
    public Optional<Reserva> findByIdForUpdate(Long id) {
        return repositorio.findByIdForUpdate(id);
    }

    @Override
    public Optional<Reserva> findByOrderId(String orderId) {
        return repositorio.findByOrderId(orderId);
    }

    @Override
    public Optional<Reserva> findByIdempotencyKey(String idempotencyKey) {
        return repositorio.findByIdempotencyKey(idempotencyKey);
    }
}

package co.edu.udistrital.acciones.m6portafolio.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m6portafolio.application.port.out.ReservaPosicionRepositoryPort;
import co.edu.udistrital.acciones.m6portafolio.domain.ReservaPosicion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
interface SpringDataReservaPosicionRepository extends JpaRepository<ReservaPosicion, Long> {

    Optional<ReservaPosicion> findByOrderId(String orderId);

    Optional<ReservaPosicion> findByIdempotencyKey(String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReservaPosicion r where r.id = :id")
    Optional<ReservaPosicion> buscarPorIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReservaPosicion r where r.orderId = :orderId")
    Optional<ReservaPosicion> buscarPorOrderIdForUpdate(@Param("orderId") String orderId);
}

@Repository
public class JpaReservaPosicionRepositoryAdapter implements ReservaPosicionRepositoryPort {

    private final SpringDataReservaPosicionRepository repositorio;

    public JpaReservaPosicionRepositoryAdapter(SpringDataReservaPosicionRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public Optional<ReservaPosicion> findById(Long id) {
        return repositorio.findById(id);
    }

    @Override
    public Optional<ReservaPosicion> findByIdForUpdate(Long id) {
        return repositorio.buscarPorIdForUpdate(id);
    }

    @Override
    public Optional<ReservaPosicion> findByOrderId(String orderId) {
        return repositorio.findByOrderId(orderId);
    }

    @Override
    public Optional<ReservaPosicion> findByOrderIdForUpdate(String orderId) {
        return repositorio.buscarPorOrderIdForUpdate(orderId);
    }

    @Override
    public Optional<ReservaPosicion> findByIdempotencyKey(String idempotencyKey) {
        return repositorio.findByIdempotencyKey(idempotencyKey);
    }

    @Override
    public ReservaPosicion save(ReservaPosicion reserva) {
        return repositorio.save(reserva);
    }
}

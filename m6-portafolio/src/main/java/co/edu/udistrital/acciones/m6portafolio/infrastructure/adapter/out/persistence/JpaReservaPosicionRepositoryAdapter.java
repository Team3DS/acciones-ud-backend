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
import java.util.UUID;

@Repository
interface SpringDataReservaPosicionRepository extends JpaRepository<ReservaPosicion, Long> {

    Optional<ReservaPosicion> findByOrdenId(Long ordenId);

    Optional<ReservaPosicion> findByClaveIdempotencia(UUID claveIdempotencia);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReservaPosicion r where r.id = :id")
    Optional<ReservaPosicion> buscarPorIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReservaPosicion r where r.ordenId = :ordenId")
    Optional<ReservaPosicion> buscarPorOrdenIdForUpdate(@Param("ordenId") Long ordenId);
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
        return repositorio.findByOrdenId(Long.valueOf(orderId));
    }

    @Override
    public Optional<ReservaPosicion> findByOrderIdForUpdate(String orderId) {
        return repositorio.buscarPorOrdenIdForUpdate(Long.valueOf(orderId));
    }

    @Override
    public Optional<ReservaPosicion> findByIdempotencyKey(String idempotencyKey) {
        return repositorio.findByClaveIdempotencia(UUID.fromString(idempotencyKey));
    }

    @Override
    public ReservaPosicion save(ReservaPosicion reserva) {
        return repositorio.save(reserva);
    }
}

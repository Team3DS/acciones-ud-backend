package co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.ReservaRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.EstadoReserva;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Reserva;
import jakarta.persistence.LockModeType;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
interface SpringDataReservaRepository extends JpaRepository<Reserva, Long> {

    Optional<Reserva> findByOrderId(String orderId);

    Optional<Reserva> findByIdempotencyKey(String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reserva r where r.id = :id")
    Optional<Reserva> findByIdForUpdate(@Param("id") Long id);

    @Query("select r from Reserva r where r.estado = :estado and r.montoConsumido = 0 "
            + "and r.creadoEn < :limite order by r.creadoEn asc")
    List<Reserva> findVencidasSinEjecuciones(@Param("estado") EstadoReserva estado,
                                          @Param("limite") Instant limite, Pageable pageable);
}

@Repository
public class JpaReservaRepositoryAdapter implements ReservaRepositoryPort {

    private final SpringDataReservaRepository repositorio;
    private final EntityManager entityManager;

    public JpaReservaRepositoryAdapter(SpringDataReservaRepository repositorio, EntityManager entityManager) {
        this.repositorio = repositorio;
        this.entityManager = entityManager;
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
        return repositorio.findByIdForUpdate(id).map(reserva -> {
            // findById pudo cargar esta entidad antes de esperar el bloqueo de la cuenta.
            // Recargar bajo el bloqueo evita aplicar dos fills sobre un consumo obsoleto.
            entityManager.refresh(reserva, LockModeType.PESSIMISTIC_WRITE);
            return reserva;
        });
    }

    @Override
    public Optional<Reserva> findByOrderId(String orderId) {
        return repositorio.findByOrderId(orderId);
    }

    @Override
    public Optional<Reserva> findByIdempotencyKey(String idempotencyKey) {
        return repositorio.findByIdempotencyKey(idempotencyKey);
    }

    @Override
    public List<Reserva> findVencidas(Instant limite, int lote) {
        if (lote <= 0) {
            return List.of();
        }
        return repositorio.findVencidasSinEjecuciones(EstadoReserva.PENDING, limite,
                PageRequest.of(0, lote));
    }
}

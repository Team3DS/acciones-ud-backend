package co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.ReciboRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.Recibo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
interface SpringDataReciboRepository extends JpaRepository<Recibo, Long> {

    Optional<Recibo> findByStripeInvoiceId(String stripeInvoiceId);

    @Query("""
            select r from Recibo r
            where r.suscripcionId in (select s.id from Suscripcion s where s.userId = :userId)
            """)
    Page<Recibo> findByUserId(@Param("userId") Long userId, Pageable pageable);
}

@Repository
public class JpaReciboRepositoryAdapter implements ReciboRepositoryPort {

    private final SpringDataReciboRepository repositorio;

    public JpaReciboRepositoryAdapter(SpringDataReciboRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public Optional<Recibo> findByStripeInvoiceId(String stripeInvoiceId) {
        return repositorio.findByStripeInvoiceId(stripeInvoiceId);
    }

    @Override
    public Page<Recibo> findByUserId(Long userId, Pageable pageable) {
        return repositorio.findByUserId(userId, pageable);
    }

    @Override
    public Recibo save(Recibo recibo) {
        return repositorio.save(recibo);
    }
}

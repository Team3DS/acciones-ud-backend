package co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out;

import co.edu.udistrital.acciones.m10suscripcionespagos.domain.Recibo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ReciboRepositoryPort {

    Optional<Recibo> findByStripeInvoiceId(String stripeInvoiceId);

    Page<Recibo> findByUserId(Long userId, Pageable pageable);

    Recibo save(Recibo recibo);
}

package co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.ClienteStripeRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.ClienteStripe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
interface SpringDataClienteStripeRepository extends JpaRepository<ClienteStripe, Long> {
}

@Repository
public class JpaClienteStripeRepositoryAdapter implements ClienteStripeRepositoryPort {

    private final SpringDataClienteStripeRepository repositorio;

    public JpaClienteStripeRepositoryAdapter(SpringDataClienteStripeRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public Optional<ClienteStripe> findById(Long userId) {
        return repositorio.findById(userId);
    }

    @Override
    public ClienteStripe save(ClienteStripe cliente) {
        return repositorio.save(cliente);
    }
}

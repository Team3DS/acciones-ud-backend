package co.edu.udistrital.acciones.m1identidad.infrastructure.adapter.out;

import co.edu.udistrital.acciones.m1identidad.application.port.out.UserRepositoryPort;
import co.edu.udistrital.acciones.m1identidad.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
interface SpringDataUserRepository extends JpaRepository<User, Long> {
    Optional<User> findByCorreo(String correo);
    boolean existsByCorreo(String correo);
}

public class JpaUserRepository implements UserRepositoryPort {
    private final SpringDataUserRepository repo;

    public JpaUserRepository(SpringDataUserRepository repo) {
        this.repo = repo;
    }

    @Override public Optional<User> findById(Long id) { return repo.findById(id); }
    @Override public Optional<User> findByCorreo(String correo) { return repo.findByCorreo(correo); }
    @Override public User save(User user) { return repo.save(user); }
    @Override public boolean existsByCorreo(String correo) { return repo.existsByCorreo(correo); }
}

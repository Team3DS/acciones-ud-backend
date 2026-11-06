package co.edu.udistrital.acciones.m1identidad.infrastructure.adapter.out;

import co.edu.udistrital.acciones.m1identidad.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserJpaRepository extends JpaRepository<User, Long> {
    Optional<User> findByCorreo(String correo);
    boolean existsByCorreo(String correo);
}

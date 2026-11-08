package co.edu.udistrital.acciones.m1identidad.application.port.out;

import co.edu.udistrital.acciones.m1identidad.domain.User;
import java.util.Optional;

public interface UserRepositoryPort {
    Optional<User> findById(Long id);
    Optional<User> findByCorreo(String correo);
    User save(User user);
    boolean existsByCorreo(String correo);
}

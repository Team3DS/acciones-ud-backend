package co.edu.udistrital.acciones.m1identidad.application.usecase;

import co.edu.udistrital.acciones.m1identidad.application.port.out.UserRepositoryPort;
import co.edu.udistrital.acciones.m1identidad.domain.User;

public class RegisterUserUseCase {
    private final UserRepositoryPort userRepository;

    public RegisterUserUseCase(UserRepositoryPort userRepository) {
        this.userRepository = userRepository;
    }

    public User execute(User user) {
        if (userRepository.existsByCorreo(user.getCorreo())) {
            throw new IllegalArgumentException("Correo ya registrado");
        }
        return userRepository.save(user);
    }
}

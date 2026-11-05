package co.edu.udistrital.acciones.m1identidad.application.usecase;

import co.edu.udistrital.acciones.m1identidad.application.port.out.UserRepositoryPort;
import co.edu.udistrital.acciones.m1identidad.domain.User;
import co.edu.udistrital.acciones.m1identidad.infrastructure.service.CifradoService;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class RegisterUserUseCase {
    private final UserRepositoryPort userRepository;
    private final CifradoService cifradoService;

    public RegisterUserUseCase(UserRepositoryPort userRepository, CifradoService cifradoService) {
        this.userRepository = userRepository;
        this.cifradoService = cifradoService;
    }

    public User execute(User user) {
        if (userRepository.existsByCorreo(user.getCorreo())) {
            throw new IllegalArgumentException("Correo ya registrado");
        }
        user.setCreadoEn(OffsetDateTime.now());
        user.setActualizadoEn(OffsetDateTime.now());
        user.setPoliticaAceptadaEn(OffsetDateTime.now());
        return userRepository.save(user);
    }
}
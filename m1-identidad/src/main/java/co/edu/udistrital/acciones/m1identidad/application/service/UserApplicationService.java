package co.edu.udistrital.acciones.m1identidad.application.service;

import co.edu.udistrital.acciones.m1identidad.application.dto.RegisterUserRequest;
import co.edu.udistrital.acciones.m1identidad.application.port.out.UserRepositoryPort;
import co.edu.udistrital.acciones.m1identidad.application.usecase.RegisterUserUseCase;
import co.edu.udistrital.acciones.m1identidad.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@Transactional
public class UserApplicationService {
    private final UserRepositoryPort userRepository;
    private final RegisterUserUseCase registerUserUseCase;

    public UserApplicationService(UserRepositoryPort userRepository) {
        this.userRepository = userRepository;
        this.registerUserUseCase = new RegisterUserUseCase(userRepository);
    }

    public User register(RegisterUserRequest req) {
        User user = new User();
        user.setNombres(req.nombres());
        user.setApellidos(req.apellidos());
        // Mapeo simplificado, en producción usar cifrado y hash
        return registerUserUseCase.execute(user);
    }
}

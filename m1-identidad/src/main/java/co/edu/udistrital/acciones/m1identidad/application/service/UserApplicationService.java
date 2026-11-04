package co.edu.udistrital.acciones.m1identidad.application.service;

import co.edu.udistrital.acciones.m1identidad.application.dto.LoginRequest;
import co.edu.udistrital.acciones.m1identidad.application.dto.RegisterUserRequest;
import co.edu.udistrital.acciones.m1identidad.application.port.out.UserRepositoryPort;
import co.edu.udistrital.acciones.m1identidad.application.usecase.RegisterUserUseCase;
import co.edu.udistrital.acciones.m1identidad.domain.User;
import co.edu.udistrital.acciones.m1identidad.infrastructure.service.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;

@Service
@Transactional
public class UserApplicationService {
    private final UserRepositoryPort userRepository;
    private final RegisterUserUseCase registerUserUseCase;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserApplicationService(UserRepositoryPort userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.registerUserUseCase = new RegisterUserUseCase(userRepository);
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public User register(RegisterUserRequest req) {
        User user = new User();
        user.setNombres(req.nombres());
        user.setApellidos(req.apellidos());
        user.setCorreo(req.correo());
        user.setNivelInversionista(req.nivelInversionista());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        return registerUserUseCase.execute(user);
    }

    public Map<String, String> login(LoginRequest req) {
        User user = userRepository.findByCorreo(req.correo())
                .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas"));
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Credenciales inválidas");
        }
        String token = jwtService.generateAccessToken(user.getId());
        return Map.of("accessToken", token, "userId", String.valueOf(user.getId()));
    }
}

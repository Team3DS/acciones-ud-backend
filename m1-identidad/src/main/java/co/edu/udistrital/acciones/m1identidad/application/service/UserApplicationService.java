package co.edu.udistrital.acciones.m1identidad.application.service;

import co.edu.udistrital.acciones.m1identidad.application.dto.LoginRequest;
import co.edu.udistrital.acciones.m1identidad.application.dto.RegisterUserRequest;
import co.edu.udistrital.acciones.m1identidad.application.port.out.UserRepositoryPort;
import co.edu.udistrital.acciones.m1identidad.application.usecase.RegisterUserUseCase;
import co.edu.udistrital.acciones.m1identidad.domain.User;
import co.edu.udistrital.acciones.m1identidad.infrastructure.service.JwtService;
import co.edu.udistrital.acciones.m1identidad.infrastructure.service.RefreshTokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class UserApplicationService {
    private final UserRepositoryPort userRepository;
    private final RegisterUserUseCase registerUserUseCase;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public UserApplicationService(UserRepositoryPort userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.registerUserUseCase = new RegisterUserUseCase(userRepository);
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
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
        String accessToken = jwtService.generateAccessToken(user.getId());
        String refreshToken = UUID.randomUUID().toString();
        refreshTokenService.store(user.getId(), refreshToken);
        return Map.of("accessToken", accessToken, "refreshToken", refreshToken, "userId", String.valueOf(user.getId()));
    }
}

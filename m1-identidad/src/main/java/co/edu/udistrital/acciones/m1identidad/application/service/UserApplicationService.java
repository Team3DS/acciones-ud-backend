package co.edu.udistrital.acciones.m1identidad.application.service;

import co.edu.udistrital.acciones.m1identidad.application.dto.LoginRequest;
import co.edu.udistrital.acciones.m1identidad.application.dto.RegisterUserRequest;
import co.edu.udistrital.acciones.m1identidad.application.port.out.UserRepositoryPort;
import co.edu.udistrital.acciones.m1identidad.application.usecase.RegisterUserUseCase;
import co.edu.udistrital.acciones.m1identidad.domain.User;
import co.edu.udistrital.acciones.m1identidad.infrastructure.service.CifradoService;
import co.edu.udistrital.acciones.m1identidad.infrastructure.service.JwtService;
import co.edu.udistrital.acciones.m1identidad.infrastructure.service.MfaService;
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
    private final MfaService mfaService;
    private final CifradoService cifradoService;

    public UserApplicationService(UserRepositoryPort userRepository,
                                  RegisterUserUseCase registerUserUseCase,
                                  PasswordEncoder passwordEncoder,
                                  JwtService jwtService,
                                  RefreshTokenService refreshTokenService,
                                  MfaService mfaService,
                                  CifradoService cifradoService) {
        this.userRepository = userRepository;
        this.registerUserUseCase = registerUserUseCase;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.mfaService = mfaService;
        this.cifradoService = cifradoService;
    }

    public User register(RegisterUserRequest req) {
        User user = new User();
        user.setNombres(req.nombres());
        user.setApellidos(req.apellidos());
        user.setCorreo(req.correo());
        user.setNivelInversionista(req.nivelInversionista());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setTipoDocumento(req.tipoDocumento());
        user.setNumeroDocumentoCifrado(cifradoService.encrypt(req.numeroDocumento()));
        user.setNumeroDocumentoHash(cifradoService.hmacSha256(req.numeroDocumento()).getBytes());
        user.setCelularCifrado(cifradoService.encrypt(req.celular()));
        user.setIpRegistro("0.0.0.0");
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
        mfaService.generateAndStore(user.getId());
        return Map.of("accessToken", accessToken, "refreshToken", refreshToken, "userId", String.valueOf(user.getId()));
    }

    public boolean verifyMfa(Long userId, String code) {
        return mfaService.verify(userId, code);
    }

    public void logout(Long userId) {
        refreshTokenService.revoke(userId);
    }
}
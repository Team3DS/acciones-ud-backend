package co.edu.udistrital.acciones.m1identidad.application.service;

import co.edu.udistrital.acciones.m1identidad.application.dto.LoginRequest;
import co.edu.udistrital.acciones.m1identidad.application.dto.RegisterUserRequest;
import co.edu.udistrital.acciones.m1identidad.application.port.out.MfaNotificationPort;
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

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
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
    private final MfaNotificationPort mfaNotification;

    public UserApplicationService(UserRepositoryPort userRepository,
                                  RegisterUserUseCase registerUserUseCase,
                                  PasswordEncoder passwordEncoder,
                                  JwtService jwtService,
                                  RefreshTokenService refreshTokenService,
                                  MfaService mfaService,
                                  CifradoService cifradoService,
                                  MfaNotificationPort mfaNotification) {
        this.userRepository = userRepository;
        this.registerUserUseCase = registerUserUseCase;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.mfaService = mfaService;
        this.cifradoService = cifradoService;
        this.mfaNotification = mfaNotification;
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

    /**
     * Primer paso del inicio de sesión: valida las credenciales, genera el código MFA y lo envía al correo
     * del usuario a través de M8. Los tokens se entregan solo después de verificar el código (RNF de MFA).
     */
    public Map<String, Object> login(LoginRequest req) {
        User user = userRepository.findByCorreo(req.correo())
                .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas"));
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Credenciales inválidas");
        }
        String code = mfaService.generateAndStore(user.getId());
        Instant expiresAt = Instant.now().plus(MfaService.TTL);
        mfaNotification.codigoSolicitado(user.getId(), user.getCorreo(), code, expiresAt);

        Map<String, Object> challenge = new LinkedHashMap<>();
        challenge.put("userId", user.getId());
        challenge.put("requiresMfa", true);
        challenge.put("expiresAt", expiresAt.toString());
        return challenge;
    }

    /**
     * Segundo paso: si el código es correcto emite el access token y el refresh token.
     * Si no, responde {@code verified=false} sin tokens.
     */
    public Map<String, Object> verifyMfa(Long userId, String code) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (userId == null || code == null || !mfaService.verify(userId, code)) {
            result.put("verified", false);
            return result;
        }
        String accessToken = jwtService.generateAccessToken(userId);
        String refreshToken = UUID.randomUUID().toString();
        refreshTokenService.store(userId, refreshToken);
        result.put("verified", true);
        result.put("userId", userId);
        result.put("accessToken", accessToken);
        result.put("refreshToken", refreshToken);
        result.put("tokenType", "Bearer");
        return result;
    }

    public void logout(Long userId) {
        refreshTokenService.revoke(userId);
    }

    public co.edu.udistrital.acciones.m1identidad.application.dto.UserInfoResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con id: " + userId));
        return new co.edu.udistrital.acciones.m1identidad.application.dto.UserInfoResponse(
                user.getId(),
                user.getCorreo(),
                user.getNombres(),
                user.getApellidos(),
                user.getNivelInversionista(),
                Set.of("INVERSIONISTA"),
                user.getEstadoUsuario(),
                Boolean.TRUE.equals(user.getSolicitaReporteMensual())
        );
    }
}
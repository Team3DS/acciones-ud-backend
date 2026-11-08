package co.edu.udistrital.acciones.m1identidad.unit;

import co.edu.udistrital.acciones.m1identidad.application.dto.LoginRequest;
import co.edu.udistrital.acciones.m1identidad.application.dto.RegisterUserRequest;
import co.edu.udistrital.acciones.m1identidad.application.port.out.UserRepositoryPort;
import co.edu.udistrital.acciones.m1identidad.application.service.UserApplicationService;
import co.edu.udistrital.acciones.m1identidad.application.usecase.RegisterUserUseCase;
import co.edu.udistrital.acciones.m1identidad.domain.User;
import co.edu.udistrital.acciones.m1identidad.infrastructure.service.CifradoService;
import co.edu.udistrital.acciones.m1identidad.infrastructure.service.JwtService;
import co.edu.udistrital.acciones.m1identidad.infrastructure.service.MfaService;
import co.edu.udistrital.acciones.m1identidad.infrastructure.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserApplicationServiceTest {

    private UserRepositoryPort userRepository;
    private RegisterUserUseCase registerUserUseCase;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private RefreshTokenService refreshTokenService;
    private MfaService mfaService;
    private CifradoService cifradoService;
    private UserApplicationService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepositoryPort.class);
        registerUserUseCase = mock(RegisterUserUseCase.class);
        passwordEncoder = new BCryptPasswordEncoder();
        jwtService = mock(JwtService.class);
        refreshTokenService = mock(RefreshTokenService.class);
        mfaService = mock(MfaService.class);
        cifradoService = mock(CifradoService.class);
        service = new UserApplicationService(userRepository, registerUserUseCase, passwordEncoder, jwtService, refreshTokenService, mfaService, cifradoService);
    }

    @Test
    void registerEncodesPasswordAndDelegatesToUseCase() throws Exception {
        RegisterUserRequest req = new RegisterUserRequest(
                "Juan", "Perez", "CC", "12345678", "juan@test.com", "password123", "PRINCIPIANTE", "3001234567"
        );

        User savedUser = User.create();
        savedUser.setCorreo("juan@test.com");
        savedUser.setNombres("Juan");
        savedUser.setApellidos("Perez");
        savedUser.setNivelInversionista("PRINCIPIANTE");
        setId(savedUser, 1L);

        when(cifradoService.encrypt(anyString())).thenReturn(new byte[]{1, 2, 3});
        when(cifradoService.hmacSha256(anyString())).thenReturn("hash");
        when(registerUserUseCase.execute(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            setId(u, 1L);
            return u;
        });

        User result = service.register(req);

        assertNotNull(getId(result));
        assertEquals("juan@test.com", result.getCorreo());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(registerUserUseCase).execute(captor.capture());
        User captured = captor.getValue();
        assertTrue(passwordEncoder.matches("password123", captured.getPasswordHash()));
    }

    @Test
    void loginGeneratesTokensWhenCredentialsValid() throws Exception {
        User user = User.create();
        setId(user, 42L);
        user.setCorreo("login@test.com");
        user.setPasswordHash(passwordEncoder.encode("secret"));

        when(userRepository.findByCorreo("login@test.com")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(42L)).thenReturn("access-token-123");
        when(mfaService.generateAndStore(42L)).thenReturn("123456");

        Map<String, String> tokens = service.login(new LoginRequest("login@test.com", "secret"));

        assertEquals("access-token-123", tokens.get("accessToken"));
        assertNotNull(tokens.get("refreshToken"));
        assertEquals("42", tokens.get("userId"));
        verify(refreshTokenService).store(eq(42L), any(String.class));
        verify(mfaService).generateAndStore(42L);
    }

    @Test
    void loginThrowsWhenUserNotFound() {
        when(userRepository.findByCorreo("none@test.com")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.login(new LoginRequest("none@test.com", "secret")));

        assertEquals("Credenciales inválidas", ex.getMessage());
    }

    @Test
    void loginThrowsWhenPasswordMismatch() throws Exception {
        User user = User.create();
        setId(user, 1L);
        user.setCorreo("login@test.com");
        user.setPasswordHash(passwordEncoder.encode("correct"));

        when(userRepository.findByCorreo("login@test.com")).thenReturn(Optional.of(user));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.login(new LoginRequest("login@test.com", "wrong")));

        assertEquals("Credenciales inválidas", ex.getMessage());
    }

    @Test
    void verifyMfaDelegatesToMfaService() {
        when(mfaService.verify(1L, "123456")).thenReturn(true);

        boolean result = service.verifyMfa(1L, "123456");

        assertTrue(result);
        verify(mfaService).verify(1L, "123456");
    }

    @Test
    void logoutRevokesRefreshToken() {
        service.logout(1L);

        verify(refreshTokenService).revoke(1L);
    }

    private Long getId(User user) throws Exception {
        Field field = User.class.getDeclaredField("id");
        field.setAccessible(true);
        return (Long) field.get(user);
    }

    private void setId(User user, Long id) throws Exception {
        Field field = User.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(user, id);
    }

    private static String anyString() {
        return org.mockito.ArgumentMatchers.anyString();
    }
}
package co.edu.udistrital.acciones.m1identidad.unit;

import co.edu.udistrital.acciones.m1identidad.application.dto.LoginRequest;
import co.edu.udistrital.acciones.m1identidad.application.dto.RegisterUserRequest;
import co.edu.udistrital.acciones.m1identidad.application.port.out.MfaNotificationPort;
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
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
    private MfaNotificationPort mfaNotification;
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
        mfaNotification = mock(MfaNotificationPort.class);
        service = new UserApplicationService(userRepository, registerUserUseCase, passwordEncoder, jwtService, refreshTokenService, mfaService, cifradoService, mfaNotification);
    }

    @Test
    void profileIncludesAssignedRoles() throws Exception {
        User user = User.create();
        setId(user, 7L);
        user.setCorreo("admin@example.com");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(jwtService.rolesDe(7L)).thenReturn(java.util.List.of("ADMINISTRADOR", "INVERSIONISTA"));

        var profile = service.getUserProfile(7L);

        assertEquals(java.util.Set.of("ADMINISTRADOR", "INVERSIONISTA"), profile.roles());
    }

    @Test
    void notificationContactUsesIdentityRepository() {
        User user = User.create();
        user.setCorreo("usuario@example.com");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        assertEquals(Optional.of("usuario@example.com"), service.notificationEmail(7L));
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
    void loginSendsMfaCodeAndReturnsChallengeWithoutTokens() throws Exception {
        User user = User.create();
        setId(user, 42L);
        user.setCorreo("login@test.com");
        user.setPasswordHash(passwordEncoder.encode("secret"));

        when(userRepository.findByCorreoForUpdate("login@test.com")).thenReturn(Optional.of(user));
        when(mfaService.generateAndStore(42L)).thenReturn("123456");

        Instant before = Instant.now();
        Map<String, Object> challenge = service.login(new LoginRequest("login@test.com", "secret"));

        assertEquals(42L, challenge.get("userId"));
        assertEquals(true, challenge.get("requiresMfa"));
        assertFalse(challenge.containsKey("accessToken"), "El access token solo se entrega tras verificar el MFA");
        assertFalse(challenge.containsKey("refreshToken"), "El refresh token solo se entrega tras verificar el MFA");

        ArgumentCaptor<Instant> expiry = ArgumentCaptor.forClass(Instant.class);
        verify(mfaNotification).codigoSolicitado(eq(42L), eq("login@test.com"), eq("123456"), expiry.capture());
        Duration ttl = Duration.between(before, expiry.getValue());
        assertTrue(ttl.compareTo(Duration.ofMinutes(15)) < 0, "M8 rechaza códigos con vigencia mayor a 15 minutos");
        assertEquals(expiry.getValue().toString(), challenge.get("expiresAt"));
        verify(jwtService, never()).generateAccessToken(anyLong());
        verify(refreshTokenService, never()).store(anyLong(), any(String.class));
    }

    @Test
    void loginDoesNotSendCodeWhenPasswordIsWrong() throws Exception {
        User user = User.create();
        setId(user, 42L);
        user.setCorreo("login@test.com");
        user.setPasswordHash(passwordEncoder.encode("secret"));
        when(userRepository.findByCorreoForUpdate("login@test.com")).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class,
                () -> service.login(new LoginRequest("login@test.com", "wrong")));

        verify(mfaService, never()).generateAndStore(anyLong());
        verify(mfaNotification, never()).codigoSolicitado(anyLong(), anyString(), anyString(), any(Instant.class));
    }

    @Test
    void loginThrowsWhenUserNotFound() {
        when(userRepository.findByCorreoForUpdate("none@test.com")).thenReturn(Optional.empty());

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

        when(userRepository.findByCorreoForUpdate("login@test.com")).thenReturn(Optional.of(user));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.login(new LoginRequest("login@test.com", "wrong")));

        assertEquals("Credenciales inválidas", ex.getMessage());
    }

    @Test
    void verifyMfaIssuesTokensWhenCodeIsValid() {
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(User.create()));
        when(mfaService.verify(1L, "123456")).thenReturn(true);
        when(jwtService.generateAccessToken(1L)).thenReturn("access-token-123");

        Map<String, Object> result = service.verifyMfa(1L, "123456");

        assertEquals(true, result.get("verified"));
        assertEquals(1L, result.get("userId"));
        assertEquals("access-token-123", result.get("accessToken"));
        assertNotNull(result.get("refreshToken"));
        assertEquals("Bearer", result.get("tokenType"));
        verify(refreshTokenService).store(1L, (String) result.get("refreshToken"));
    }

    @Test
    void verifyMfaReturnsNoTokensWhenCodeIsInvalid() {
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(User.create()));
        when(mfaService.verify(1L, "000000")).thenReturn(false);

        Map<String, Object> result = service.verifyMfa(1L, "000000");

        assertEquals(false, result.get("verified"));
        assertFalse(result.containsKey("accessToken"));
        assertFalse(result.containsKey("refreshToken"));
        verify(jwtService, never()).generateAccessToken(anyLong());
        verify(refreshTokenService, never()).store(anyLong(), any(String.class));
    }

    @Test
    void verifyMfaRejectsMissingData() {
        assertEquals(false, service.verifyMfa(null, "123456").get("verified"));
        assertEquals(false, service.verifyMfa(1L, null).get("verified"));
        verify(mfaService, never()).verify(any(), any());
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

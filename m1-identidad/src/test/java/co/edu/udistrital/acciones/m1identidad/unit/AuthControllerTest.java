package co.edu.udistrital.acciones.m1identidad.unit;

import co.edu.udistrital.acciones.m1identidad.application.dto.LoginRequest;
import co.edu.udistrital.acciones.m1identidad.application.dto.MfaVerifyRequest;
import co.edu.udistrital.acciones.m1identidad.application.dto.RefreshRequest;
import co.edu.udistrital.acciones.m1identidad.application.dto.RegisterUserRequest;
import co.edu.udistrital.acciones.m1identidad.application.service.UserApplicationService;
import co.edu.udistrital.acciones.m1identidad.domain.User;
import co.edu.udistrital.acciones.m1identidad.infrastructure.adapter.in.AuthController;
import co.edu.udistrital.acciones.m1identidad.infrastructure.service.JwtService;
import co.edu.udistrital.acciones.m1identidad.infrastructure.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    private UserApplicationService service;
    private JwtService jwtService;
    private RefreshTokenService refreshTokenService;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        service = mock(UserApplicationService.class);
        jwtService = mock(JwtService.class);
        refreshTokenService = mock(RefreshTokenService.class);
        controller = new AuthController(service, jwtService, refreshTokenService);
    }

    @Test
    void registerReturns200WithUserIdAndEmail() throws Exception {
        RegisterUserRequest req = new RegisterUserRequest(
                "Juan", "Perez", "CC", "12345678", "juan@test.com", "password123", "PRINCIPIANTE", "3001234567"
        );
        User user = User.create();
        user.setCorreo("juan@test.com");
        setId(user, 1L);

        when(service.register(req)).thenReturn(user);

        ResponseEntity<?> response = controller.register(req);

        assertEquals(200, response.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertNotNull(body);
        assertEquals(1L, body.get("id"));
        assertEquals("juan@test.com", body.get("correo"));
    }

    @Test
    void loginReturns200WithMfaChallenge() {
        LoginRequest req = new LoginRequest("login@test.com", "secret");

        when(service.login(req)).thenReturn(Map.<String, Object>of(
                "userId", 42L,
                "requiresMfa", true,
                "expiresAt", "2026-11-21T22:05:00Z"
        ));

        ResponseEntity<?> response = controller.login(req);

        assertEquals(200, response.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertNotNull(body);
        assertEquals(42L, body.get("userId"));
        assertEquals(true, body.get("requiresMfa"));
        assertNull(body.get("accessToken"));
    }

    @Test
    void mfaVerifyReturnsVerifiedTrue() {
        MfaVerifyRequest req = new MfaVerifyRequest(1L, "123456");

        when(service.verifyMfa(1L, "123456")).thenReturn(Map.<String, Object>of(
                "verified", true,
                "userId", 1L,
                "accessToken", "access-123",
                "refreshToken", "refresh-456"
        ));

        ResponseEntity<?> response = controller.mfaVerify(req);

        assertEquals(200, response.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(true, body.get("verified"));
        assertEquals("access-123", body.get("accessToken"));
        assertEquals("refresh-456", body.get("refreshToken"));
    }

    @Test
    void mfaVerifyReturnsVerifiedFalse() {
        MfaVerifyRequest req = new MfaVerifyRequest(1L, "000000");

        when(service.verifyMfa(1L, "000000")).thenReturn(Map.<String, Object>of("verified", false));

        ResponseEntity<?> response = controller.mfaVerify(req);

        assertEquals(200, response.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(false, body.get("verified"));
    }

    @Test
    void refreshReturns200WhenTokenValid() {
        RefreshRequest req = new RefreshRequest(42L, "valid-refresh");

        when(refreshTokenService.get(42L)).thenReturn("valid-refresh");
        when(jwtService.generateAccessToken(42L)).thenReturn("new-access-token");

        ResponseEntity<?> response = controller.refresh(req);

        assertEquals(200, response.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("new-access-token", body.get("accessToken"));
    }

    @Test
    void refreshReturns401WhenTokenInvalid() {
        RefreshRequest req = new RefreshRequest(42L, "wrong-refresh");

        when(refreshTokenService.get(42L)).thenReturn("valid-refresh");

        ResponseEntity<?> response = controller.refresh(req);

        assertEquals(401, response.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("invalid refresh", body.get("error"));
    }

    @Test
    void refreshReturns401WhenTokenMissing() {
        RefreshRequest req = new RefreshRequest(42L, "any-refresh");

        when(refreshTokenService.get(42L)).thenReturn(null);

        ResponseEntity<?> response = controller.refresh(req);

        assertEquals(401, response.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("invalid refresh", body.get("error"));
    }

    @Test
    void logoutReturns200AndDelegatesToService() {
        RefreshRequest req = new RefreshRequest(1L, "refresh-token");
        when(refreshTokenService.get(1L)).thenReturn("refresh-token");

        ResponseEntity<?> response = controller.logout(req);

        assertEquals(200, response.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("Logged out", body.get("message"));
        verify(service).logout(1L);
    }

    @Test
    void logoutReturns401ForAnotherUsersRefreshTokenWithoutRevoking() {
        when(refreshTokenService.get(42L)).thenReturn("token-of-user-42");

        ResponseEntity<?> response = controller.logout(new RefreshRequest(42L, "token-of-user-1"));

        assertEquals(401, response.getStatusCode().value());
        assertEquals("invalid refresh", ((Map<?, ?>) response.getBody()).get("error"));
        verifyNoInteractions(service, jwtService);
    }

    @Test
    void logoutReturns401ForUnknownOrExpiredRefreshTokenWithoutRevoking() {
        ResponseEntity<?> response = controller.logout(new RefreshRequest(42L, "expired-token"));

        assertEquals(401, response.getStatusCode().value());
        verifyNoInteractions(service, jwtService);
    }

    @Test
    void logoutReturns401ForMissingTokenOrUserWithoutLookingUpSession() {
        assertEquals(401, controller.logout(new RefreshRequest(42L, null)).getStatusCode().value());
        assertEquals(401, controller.logout(new RefreshRequest(42L, " ")).getStatusCode().value());
        assertEquals(401, controller.logout(new RefreshRequest(null, "token")).getStatusCode().value());
        assertEquals(401, controller.logout(new RefreshRequest(0L, "token")).getStatusCode().value());

        verifyNoInteractions(service, jwtService, refreshTokenService);
    }

    private void setId(User user, Long id) throws Exception {
        Field field = User.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(user, id);
    }
}

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
    void loginReturns200WithTokens() {
        LoginRequest req = new LoginRequest("login@test.com", "secret");

        when(service.login(req)).thenReturn(Map.of(
                "accessToken", "access-123",
                "refreshToken", "refresh-456",
                "userId", "42"
        ));

        ResponseEntity<?> response = controller.login(req);

        assertEquals(200, response.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertNotNull(body);
        assertEquals("access-123", body.get("accessToken"));
        assertEquals("refresh-456", body.get("refreshToken"));
        assertEquals("42", body.get("userId"));
    }

    @Test
    void mfaVerifyReturnsVerifiedTrue() {
        MfaVerifyRequest req = new MfaVerifyRequest(1L, "123456");

        when(service.verifyMfa(1L, "123456")).thenReturn(true);

        ResponseEntity<?> response = controller.mfaVerify(req);

        assertEquals(200, response.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(true, body.get("verified"));
    }

    @Test
    void mfaVerifyReturnsVerifiedFalse() {
        MfaVerifyRequest req = new MfaVerifyRequest(1L, "000000");

        when(service.verifyMfa(1L, "000000")).thenReturn(false);

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

        ResponseEntity<?> response = controller.logout(req);

        assertEquals(200, response.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("Logged out", body.get("message"));
        verify(service).logout(1L);
    }

    private void setId(User user, Long id) throws Exception {
        Field field = User.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(user, id);
    }
}
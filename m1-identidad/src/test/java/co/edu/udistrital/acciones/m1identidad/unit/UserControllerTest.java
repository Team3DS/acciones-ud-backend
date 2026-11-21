package co.edu.udistrital.acciones.m1identidad.unit;

import co.edu.udistrital.acciones.m1identidad.application.dto.UserInfoResponse;
import co.edu.udistrital.acciones.m1identidad.application.service.UserApplicationService;
import co.edu.udistrital.acciones.m1identidad.infrastructure.adapter.in.UserController;
import co.edu.udistrital.acciones.m1identidad.infrastructure.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserApplicationService userService;

    @Mock
    private JwtService jwtService;

    private UserController controller;

    @BeforeEach
    void setUp() {
        controller = new UserController(userService, jwtService);
    }

    @Test
    void healthReturnsOk() {
        String result = controller.health();

        assertEquals("OK", result);
    }

    @Test
    void meConTokenValidoRetornaPerfil() {
        when(jwtService.parseUserId("valid-token")).thenReturn(10L);
        UserInfoResponse profile = new UserInfoResponse(
                10L, "test@udistrital.edu.co", "Juan", "Perez", "PRINCIPIANTE", Set.of("INVERSIONISTA"), "ACTIVO", true
        );
        when(userService.getUserProfile(10L)).thenReturn(profile);

        ResponseEntity<UserInfoResponse> response = controller.me("Bearer valid-token", null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("test@udistrital.edu.co", response.getBody().correo());
        assertEquals("Juan", response.getBody().nombres());
    }

    @Test
    void meSinTokenNiHeaderRetorna401() {
        ResponseEntity<UserInfoResponse> response = controller.me(null, null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}
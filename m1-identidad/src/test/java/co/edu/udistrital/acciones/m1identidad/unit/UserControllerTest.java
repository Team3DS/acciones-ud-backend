package co.edu.udistrital.acciones.m1identidad.unit;

import co.edu.udistrital.acciones.m1identidad.application.port.out.UserRepositoryPort;
import co.edu.udistrital.acciones.m1identidad.application.service.UserApplicationService;
import co.edu.udistrital.acciones.m1identidad.domain.User;
import co.edu.udistrital.acciones.m1identidad.infrastructure.adapter.in.UserController;
import co.edu.udistrital.acciones.m1identidad.infrastructure.service.JwtService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    UserApplicationService userApplicationService;

    @Mock
    UserRepositoryPort userRepository;

    @Mock
    JwtService jwtService;

    private UserController controller;

    @BeforeEach
    void setUp() {
        controller = new UserController(userApplicationService, userRepository, jwtService);
    }

    @Test
    void healthReturnsOk() {
        String result = controller.health();
        assertEquals("OK", result);
    }

    @Test
    void getUserByIdNotFoundReturns404() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        ResponseEntity<?> result = controller.getUserById(999L);
        assertEquals(404, result.getStatusCode().value());
    }

    @Test
    void listUsersReturnsOk() {
        ResponseEntity<?> result = controller.listUsers();
        assertEquals(200, result.getStatusCode().value());
    }

    @Test
    void updateUserRolesReturnsOk() {
        ResponseEntity<?> result = controller.updateUserRoles(1L, Map.of("roles", List.of("ADMINISTRADOR")));
        assertEquals(200, result.getStatusCode().value());
        verify(userApplicationService).assignRoles(1L, Set.of("ADMINISTRADOR"));
    }

    @Test
    void updateUserEstadoReturnsOk() {
        ResponseEntity<?> result = controller.updateUserEstado(1L, Map.of("estado", "ACTIVO", "motivo", "prueba"));
        assertEquals(200, result.getStatusCode().value());
        verify(userApplicationService).changeStatus(1L, "ACTIVO", "prueba");
    }
}

package co.edu.udistrital.acciones.m1identidad.unit;

import co.edu.udistrital.acciones.m1identidad.application.port.out.UserRepositoryPort;
import co.edu.udistrital.acciones.m1identidad.application.usecase.RegisterUserUseCase;
import co.edu.udistrital.acciones.m1identidad.domain.User;
import co.edu.udistrital.acciones.m1identidad.infrastructure.service.CifradoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegisterUserUseCaseTest {

    private UserRepositoryPort userRepository;
    private CifradoService cifradoService;
    private RegisterUserUseCase useCase;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepositoryPort.class);
        cifradoService = mock(CifradoService.class);
        useCase = new RegisterUserUseCase(userRepository, cifradoService);
    }

    @Test
    void executeSavesUserWhenCorreoDoesNotExist() throws Exception {
        User user = User.create();
        user.setCorreo("nuevo@test.com");
        user.setNombres("Juan");
        user.setApellidos("Perez");
        user.setNivelInversionista("PRINCIPIANTE");
        user.setPasswordHash("hash");
        user.setTipoDocumento("CC");
        user.setNumeroDocumentoCifrado(new byte[]{1, 2, 3});
        user.setNumeroDocumentoHash("hash".getBytes());
        user.setCelularCifrado(new byte[]{4, 5, 6});
        user.setIpRegistro("0.0.0.0");

        when(userRepository.existsByCorreo("nuevo@test.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            setId(u, 1L);
            return u;
        });

        User result = useCase.execute(user);

        assertNotNull(getId(result));
        assertEquals("nuevo@test.com", result.getCorreo());
        verify(userRepository).existsByCorreo("nuevo@test.com");
        verify(userRepository).save(user);
    }

    @Test
    void executeThrowsWhenCorreoAlreadyRegistered() {
        User user = User.create();
        user.setCorreo("existente@test.com");

        when(userRepository.existsByCorreo("existente@test.com")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> useCase.execute(user));

        assertEquals("Correo ya registrado", ex.getMessage());
        verify(userRepository).existsByCorreo("existente@test.com");
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
}
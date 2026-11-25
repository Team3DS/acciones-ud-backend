package co.edu.udistrital.acciones.m1identidad.unit;

import co.edu.udistrital.acciones.m1identidad.application.service.UserApplicationService;
import co.edu.udistrital.acciones.m1identidad.infrastructure.adapter.in.AuthController;
import co.edu.udistrital.acciones.m1identidad.infrastructure.config.GlobalExceptionHandler;
import co.edu.udistrital.acciones.m1identidad.infrastructure.service.JwtService;
import co.edu.udistrital.acciones.m1identidad.infrastructure.service.RefreshTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerHttpTest {

    private final UserApplicationService service = mock(UserApplicationService.class);
    private final JwtService jwt = mock(JwtService.class);
    private final RefreshTokenService refresh = mock(RefreshTokenService.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private LocalValidatorFactoryBean validator;
    private MockMvc http;

    @BeforeEach
    void preparar() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        http = MockMvcBuilders.standaloneSetup(new AuthController(service, jwt, refresh))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void cerrarValidador() {
        validator.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"nombres", "apellidos", "tipoDocumento", "numeroDocumento", "correo", "password", "nivelInversionista", "celular"})
    void registroConCampoObligatorioVacioDevuelve400AntesDelServicio(String campo) throws Exception {
        Map<String, Object> registro = registroValido();
        registro.put(campo, " ");

        http.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(registro)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service, jwt, refresh);
    }

    @Test
    void registroConCorreoInvalidoDevuelve400AntesDelServicio() throws Exception {
        Map<String, Object> registro = registroValido();
        registro.put("correo", "correo-invalido");

        http.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(registro)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service, jwt, refresh);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"correo\":\"no-es-correo\",\"password\":\"secret\"}", "{\"correo\":\"\",\"password\":\"secret\"}", "{\"correo\":\"user@example.com\",\"password\":\" \"}", "{}"})
    void loginInvalidoDevuelve400AntesDelServicio(String cuerpo) throws Exception {
        http.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service, jwt, refresh);
    }

    @Test
    void loginValidoConservaElContratoDeDesafioMfa() throws Exception {
        when(service.login(any())).thenReturn(Map.of("userId", 42L, "requiresMfa", true));

        http.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"user@example.com\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(42))
                .andExpect(jsonPath("$.requiresMfa").value(true));
    }

    @Test
    void logoutSinBearerRequiereElRefreshDelUsuarioObjetivo() throws Exception {
        when(refresh.get(42L)).thenReturn("refresh-42");

        http.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":42,\"refreshToken\":\"refresh-of-someone-else\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid refresh"));
        verifyNoInteractions(service, jwt);

        http.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":42,\"refreshToken\":\"refresh-42\"}"))
                .andExpect(status().isOk());
        verify(service).logout(42L);
    }

    private static Map<String, Object> registroValido() {
        return new LinkedHashMap<>(Map.of(
                "nombres", "Juan", "apellidos", "Perez", "tipoDocumento", "CC", "numeroDocumento", "12345678",
                "correo", "user@example.com", "password", "secret", "nivelInversionista", "PRINCIPIANTE", "celular", "3001234567"));
    }
}

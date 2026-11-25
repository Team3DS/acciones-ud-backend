package co.edu.udistrital.acciones.m1identidad.unit;

import co.edu.udistrital.acciones.m1identidad.application.service.InvalidPasswordResetException;
import co.edu.udistrital.acciones.m1identidad.application.service.PasswordPolicy;
import co.edu.udistrital.acciones.m1identidad.application.service.PasswordResetApplicationService;
import co.edu.udistrital.acciones.m1identidad.infrastructure.adapter.in.PasswordResetController;
import co.edu.udistrital.acciones.m1identidad.infrastructure.config.GlobalExceptionHandler;
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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PasswordResetHttpTest {
    private final PasswordResetApplicationService service = mock(PasswordResetApplicationService.class);
    private final ObjectMapper json = new ObjectMapper();
    private LocalValidatorFactoryBean validator;
    private MockMvc http;

    @BeforeEach
    void prepare() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        http = MockMvcBuilders.standaloneSetup(new PasswordResetController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).setValidator(validator).build();
    }

    @AfterEach
    void close() { validator.close(); }

    @Test
    void requestUses202WithoutChallengeOrAccountDetails() throws Exception {
        http.perform(post("/api/v1/auth/password-reset/request").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("correo", "fixture@example.test"))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.userId").doesNotExist());
        verify(service).request("fixture@example.test");
    }

    @ParameterizedTest
    @ValueSource(strings = {"short", "", "        ", "🔐🔐🔐🔐🔐🔐🔐"})
    void invalidNewPasswordNeverReachesChallenge(String password) throws Exception {
        invalidPassword(password);
    }

    @Test
    void rejectsMoreThan72Utf8BytesBeforeChallenge() throws Exception {
        invalidPassword("á".repeat(37));
        invalidPassword("a".repeat(73));
    }

    @Test
    void policyCountsCodePointsAndBytesWithoutTrimming() {
        assertThat(PasswordPolicy.isValidNewPassword("🔐".repeat(8))).isTrue();
        assertThat(PasswordPolicy.isValidNewPassword("á".repeat(36))).isTrue();
        assertThat(PasswordPolicy.isValidNewPassword("a".repeat(72))).isTrue();
        assertThat(PasswordPolicy.isValidNewPassword("🔐".repeat(19))).isFalse();
        assertThat(PasswordPolicy.isValidNewPassword("  clave  ")).isTrue();
    }

    @Test
    void invalidOrExpiredCodeReturnsGeneric401AndValidRequestPreservesPassword() throws Exception {
        doThrow(new InvalidPasswordResetException()).when(service).confirm(any());
        String body = json.writeValueAsString(Map.of("correo", "fixture@example.test", "code", "000001",
                "newPassword", "  clave  "));
        http.perform(post("/api/v1/auth/password-reset/confirm").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value(
                        "No se pudo restablecer la contraseña. Solicita un nuevo código e inténtalo de nuevo."));
        verify(service).confirm(argThat(request -> request.newPassword().equals("  clave  ")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"correo\":\"invalid\"}", "{\"correo\":\"\"}"})
    void invalidEmailIsRejectedBeforeRequest(String body) throws Exception {
        http.perform(post("/api/v1/auth/password-reset/request").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    private void invalidPassword(String password) throws Exception {
        http.perform(post("/api/v1/auth/password-reset/confirm").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("correo", "fixture@example.test", "code", "123456",
                                "newPassword", password))))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}

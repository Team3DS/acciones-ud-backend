package co.edu.udistrital.acciones.m1identidad.unit;

import co.edu.udistrital.acciones.m1identidad.application.dto.*;
import co.edu.udistrital.acciones.m1identidad.infrastructure.service.PasswordResetTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuthDtoPrivacyTest {
    @Test
    void diagnosticStringsDoNotContainCredentialsOrChallengeData() throws Exception {
        String email = "private-fixture@example.test";
        String password = "private-fixture-password";
        String code = "048213";
        for (Object value : List.of(new LoginRequest(email, password),
                new RegisterUserRequest("Name", "Surname", "CC", "123", email, password, "PRINCIPIANTE", "3000000000"),
                new MfaVerifyRequest(42L, code), new RefreshRequest(42L, password),
                new PasswordResetRequest(email), new PasswordResetConfirmRequest(email, code, password),
                new PasswordResetTokenService.Challenge(code, Instant.now().plusSeconds(300)))) {
            assertThat(value.toString()).doesNotContain(email, password, code).contains("redacted");
        }
        // Redacting diagnostics must not alter the JSON contract used by clients.
        var json = new ObjectMapper().valueToTree(new PasswordResetConfirmRequest(email, code, password));
        assertThat(json.path("newPassword").asText()).isEqualTo(password);
        assertThat(json.path("code").asText()).isEqualTo(code);
        assertThat(json.has("passwordValid")).isFalse();
    }
}

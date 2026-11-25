package co.edu.udistrital.acciones.m1identidad.unit;

import co.edu.udistrital.acciones.m1identidad.application.dto.PasswordResetConfirmRequest;
import co.edu.udistrital.acciones.m1identidad.application.port.out.PasswordResetNotificationPort;
import co.edu.udistrital.acciones.m1identidad.application.port.out.PasswordResetCompletionPort;
import co.edu.udistrital.acciones.m1identidad.application.port.out.UserRepositoryPort;
import co.edu.udistrital.acciones.m1identidad.application.service.PasswordResetApplicationService;
import co.edu.udistrital.acciones.m1identidad.domain.User;
import co.edu.udistrital.acciones.m1identidad.infrastructure.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PasswordResetApplicationServiceTest {
    private final UserRepositoryPort users = mock(UserRepositoryPort.class);
    private final PasswordResetTokenService tokens = mock(PasswordResetTokenService.class);
    private final PasswordResetNotificationPort notifications = mock(PasswordResetNotificationPort.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final RefreshTokenService refresh = mock(RefreshTokenService.class);
    private final MfaService mfa = mock(MfaService.class);
    private final PasswordResetCompletionPort completion = mock(PasswordResetCompletionPort.class);
    private final PlatformTransactionManager tx = mock(PlatformTransactionManager.class);
    private final PasswordResetApplicationService service = new PasswordResetApplicationService(
            users, tokens, notifications, encoder, refresh, mfa, completion, tx);

    @Test
    void missingAccountDoesNotIssueOrPublish() {
        when(users.findByCorreo("missing@example.test")).thenReturn(Optional.empty());
        service.request("missing@example.test");
        verifyNoInteractions(tokens, notifications, encoder, refresh, mfa);
    }

    @Test
    void cooldownDoesNotPublishAndProviderFailureDoesNotRevealAccount() throws Exception {
        User user = User.create();
        var id = User.class.getDeclaredField("id"); id.setAccessible(true); id.set(user, 42L);
        user.setCorreo("fixture@example.test");
        when(users.findByCorreo(user.getCorreo())).thenReturn(Optional.of(user));
        when(tokens.issue(42L)).thenReturn(Optional.empty());
        service.request(user.getCorreo());
        verifyNoInteractions(notifications);
        Instant expiry = Instant.now().plusSeconds(300);
        when(tokens.issue(42L)).thenReturn(Optional.of(new PasswordResetTokenService.Challenge("123456", expiry)));
        doThrow(new AmqpException("fixture unavailable")).when(notifications)
                .recuperacionSolicitada(42L, user.getCorreo(), "123456", expiry);
        assertDoesNotThrow(() -> service.request(user.getCorreo()));
    }

    @Test
    void nonHttpCallerCannotConsumeCodeWithInvalidPassword() {
        assertThrows(IllegalArgumentException.class, () -> service.confirm(
                new PasswordResetConfirmRequest("fixture@example.test", "123456", "short")));
        verifyNoInteractions(users, tokens, tx, encoder, refresh, mfa, completion);
    }
}

package co.edu.udistrital.acciones.m1identidad.application.service;

import co.edu.udistrital.acciones.m1identidad.application.dto.PasswordResetConfirmRequest;
import co.edu.udistrital.acciones.m1identidad.application.port.out.PasswordResetNotificationPort;
import co.edu.udistrital.acciones.m1identidad.application.port.out.PasswordResetCompletionPort;
import co.edu.udistrital.acciones.m1identidad.application.port.out.UserRepositoryPort;
import co.edu.udistrital.acciones.m1identidad.domain.User;
import co.edu.udistrital.acciones.m1identidad.infrastructure.service.MfaService;
import co.edu.udistrital.acciones.m1identidad.infrastructure.service.PasswordResetTokenService;
import co.edu.udistrital.acciones.m1identidad.infrastructure.service.RefreshTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;

@Service
public class PasswordResetApplicationService {
    private static final Logger LOG = LoggerFactory.getLogger(PasswordResetApplicationService.class);
    private final UserRepositoryPort users;
    private final PasswordResetTokenService tokens;
    private final PasswordResetNotificationPort notifications;
    private final PasswordEncoder encoder;
    private final RefreshTokenService refresh;
    private final MfaService mfa;
    private final PasswordResetCompletionPort completion;
    private final TransactionTemplate transactions;

    public PasswordResetApplicationService(UserRepositoryPort users, PasswordResetTokenService tokens,
            PasswordResetNotificationPort notifications, PasswordEncoder encoder,
            RefreshTokenService refresh, MfaService mfa, PasswordResetCompletionPort completion,
            PlatformTransactionManager transactionManager) {
        this.users = users;
        this.tokens = tokens;
        this.notifications = notifications;
        this.encoder = encoder;
        this.refresh = refresh;
        this.mfa = mfa;
        this.completion = completion;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    public void request(String correo) {
        // Identical response for unknown accounts, active cooldowns and provider errors.
        users.findByCorreo(correo.trim()).ifPresent(user -> tokens.issue(user.getId()).ifPresent(challenge -> {
            try {
                notifications.recuperacionSolicitada(user.getId(), user.getCorreo(),
                        challenge.code(), challenge.expiresAt());
            } catch (AmqpException unavailable) {
                LOG.warn("password_reset_notification_unavailable");
            }
        }));
    }

    public void confirm(PasswordResetConfirmRequest request) {
        // This check also protects non-HTTP callers, before accessing or consuming a challenge.
        if (!PasswordPolicy.isValidNewPassword(request.newPassword())) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres y como máximo 72 bytes UTF-8");
        }
        transactions.executeWithoutResult(status -> {
            User user = users.findByCorreoForUpdate(request.correo().trim())
                    .orElseThrow(InvalidPasswordResetException::new);
            String claim = tokens.claim(user.getId(), request.code())
                    .orElseThrow(InvalidPasswordResetException::new);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int completionStatus) {
                    try {
                        if (completionStatus == STATUS_COMMITTED) tokens.complete(user.getId(), claim);
                        else if (completionStatus == STATUS_ROLLED_BACK) tokens.release(user.getId(), claim);
                    } catch (RuntimeException unavailable) {
                        // A retained claim stays unusable until the original OTP expires.
                        LOG.warn("password_reset_challenge_cleanup_unavailable");
                    }
                }
            });
            user.setPasswordHash(encoder.encode(request.newPassword()));
            user.setActualizadoEn(OffsetDateTime.now());
            users.save(user);
            completion.register(user.getId(), user.getCorreo());
            refresh.revoke(user.getId());
            mfa.revoke(user.getId());
        });
    }
}

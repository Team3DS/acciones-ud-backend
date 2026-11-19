package co.edu.udistrital.acciones.m8notificaciones.application.service;

import co.edu.udistrital.acciones.m8notificaciones.application.port.out.AvisosRepositoryPort;
import co.edu.udistrital.acciones.m8notificaciones.application.port.out.CorreoException;
import co.edu.udistrital.acciones.m8notificaciones.application.port.out.CorreoPort;
import co.edu.udistrital.acciones.m8notificaciones.application.port.out.ServiciosPort;
import co.edu.udistrital.acciones.m8notificaciones.domain.AvisoException;
import co.edu.udistrital.acciones.m8notificaciones.domain.Correo;
import co.edu.udistrital.acciones.m8notificaciones.domain.EntregaCorreo;
import co.edu.udistrital.acciones.m8notificaciones.infrastructure.security.CifradoCorreo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;

@Service
public class EntregasService {
    private final AvisosRepositoryPort repository;
    private final CifradoCorreo encryption;
    private final CorreoPort transport;
    private final ServiciosPort services;
    private final Clock clock;
    private final TransactionTemplate transaction;

    public EntregasService(
            AvisosRepositoryPort repository,
            CifradoCorreo encryption,
            CorreoPort transport,
            ServiciosPort services,
            Clock clock,
            PlatformTransactionManager manager) {
        this.repository = repository;
        this.encryption = encryption;
        this.transport = transport;
        this.services = services;
        this.clock = clock;
        this.transaction = new TransactionTemplate(manager);
    }

    public int procesar(int limit) {
        int processed = 0;
        for (int n = 0; n < Math.min(limit, 100); n++) {
            Boolean found =
                    transaction.execute(
                            status -> {
                                var delivery = repository.bloquearCorreo();
                                delivery.ifPresent(this::enviar);
                                return delivery.isPresent();
                            });
            if (!Boolean.TRUE.equals(found)) {
                break;
            }
            processed++;
        }
        return processed;
    }

    private void enviar(EntregaCorreo delivery) {
        if (!delivery.expiresAt().isAfter(clock.instant())) {
            repository.correoFallido(delivery.id(), "EXPIRADO", "VENTANA_AGOTADA", clock.instant());
            return;
        }
        try {
            Correo mail = encryption.descifrar(delivery.encryptedContent());
            if (mail.to() == null || mail.to().isBlank()) {
                mail =
                        new Correo(
                                mail.userId(),
                                services.correoUsuario(mail.userId()),
                                mail.subject(),
                                mail.html());
                repository.fijarContenidoCorreo(delivery.id(), encryption.cifrar(mail));
            }
            repository.correoEnviado(delivery.id(), transport.enviar(delivery.id(), mail));
        } catch (CorreoException exception) {
            fallo(delivery, exception.getMessage(), exception.retryable());
        } catch (AvisoException exception) {
            fallo(delivery, "DESTINATARIO_NO_DISPONIBLE", exception.status() == 503);
        } catch (IllegalStateException exception) {
            fallo(delivery, "CONTENIDO_INVALIDO", false);
        }
    }

    private void fallo(EntregaCorreo delivery, String code, boolean retryable) {
        String state = retryable && delivery.attempts() < 4 ? "PENDIENTE" : "FALLIDO";
        repository.correoFallido(
                delivery.id(),
                state,
                code,
                clock.instant().plus(Duration.ofSeconds(30L << Math.min(delivery.attempts(), 4))));
    }
}

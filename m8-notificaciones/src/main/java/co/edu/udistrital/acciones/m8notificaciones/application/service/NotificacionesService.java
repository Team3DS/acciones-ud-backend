package co.edu.udistrital.acciones.m8notificaciones.application.service;

import co.edu.udistrital.acciones.m8notificaciones.application.port.out.AvisosRepositoryPort;
import co.edu.udistrital.acciones.m8notificaciones.domain.AvisoException;
import co.edu.udistrital.acciones.m8notificaciones.domain.Canal;
import co.edu.udistrital.acciones.m8notificaciones.domain.Correo;
import co.edu.udistrital.acciones.m8notificaciones.domain.Notificacion;
import co.edu.udistrital.acciones.m8notificaciones.domain.Preferencia;
import co.edu.udistrital.acciones.m8notificaciones.domain.TipoAviso;
import co.edu.udistrital.acciones.m8notificaciones.infrastructure.security.CifradoCorreo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class NotificacionesService {
    private final AvisosRepositoryPort repository;
    private final CifradoCorreo encryption;
    private final Clock clock;

    public NotificacionesService(
            AvisosRepositoryPort repository, CifradoCorreo encryption, Clock clock) {
        this.repository = repository;
        this.encryption = encryption;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Pagina listar(long user, int page, int size, boolean unread) {
        if (page < 0 || page > 100000 || size < 1 || size > 100) {
            throw new AvisoException(400, "Paginación inválida.");
        }
        return new Pagina(
                repository.notificaciones(user, page * size, size, unread),
                repository.total(user, unread),
                page,
                size);
    }

    public long noLeidas(long user) {
        return repository.total(user, true);
    }

    @Transactional
    public void leer(long user, long id) {
        if (!repository.marcarLeida(user, id)) {
            throw new AvisoException(404, "Notificación no encontrada.");
        }
    }

    @Transactional(readOnly = true)
    public List<Preferencia> preferencias(long user) {
        List<Preferencia> preferences = new ArrayList<>();
        for (TipoAviso type : TipoAviso.values()) {
            for (Canal channel : Canal.values()) {
                preferences.add(
                        new Preferencia(type, channel, repository.habilitada(user, type, channel)));
            }
        }
        return List.copyOf(preferences);
    }

    @Transactional
    public void preferencia(long user, Preferencia preference) {
        repository.preferencia(user, preference);
    }

    @Transactional
    public void suscribir(long user, String market) {
        repository.suscribir(user, mercado(market));
    }

    @Transactional
    public void desuscribir(long user, String market) {
        repository.desuscribir(user, mercado(market));
    }

    public List<String> mercados(long user) {
        return repository.mercados(user);
    }

    public long crear(
            UUID event,
            long user,
            TipoAviso type,
            String title,
            String message,
            Long order,
            String market,
            String email,
            Long reportId) {
        boolean app = repository.habilitada(user, type, Canal.APP);
        boolean mail = repository.habilitada(user, type, Canal.CORREO);
        if (!app && !mail) {
            return 0;
        }
        long notification =
                repository.crearNotificacion(event, user, type, title, message, order, market, app);
        if (mail) {
            encolar(
                    entregaId(event, user),
                    new Correo(user, email, title, "<p>" + HtmlUtils.htmlEscape(message) + "</p>"),
                    notification,
                    reportId,
                    clock.instant().plus(Duration.ofHours(23)));
        }
        return notification;
    }

    public void encolar(UUID id, Correo correo, Long notification, Long report, Instant expiry) {
        repository.encolarCorreo(
                id, correo.userId(), notification, report, encryption.cifrar(correo), expiry);
    }

    public static UUID entregaId(UUID event, long user) {
        return UUID.nameUUIDFromBytes((event + ":" + user).getBytes(StandardCharsets.UTF_8));
    }

    private static String mercado(String market) {
        if (!List.of("NYSE", "NASDAQ").contains(market)) {
            throw new AvisoException(400, "Mercado inválido.");
        }
        return market;
    }

    public record Pagina(List<Notificacion> content, long total, int page, int size) {
        public Pagina {
            content = List.copyOf(content);
        }
    }
}

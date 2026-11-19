package co.edu.udistrital.acciones.m8notificaciones.application.port.out;

import co.edu.udistrital.acciones.m8notificaciones.domain.AlertaPrecio;
import co.edu.udistrital.acciones.m8notificaciones.domain.Canal;
import co.edu.udistrital.acciones.m8notificaciones.domain.CondicionPrecio;
import co.edu.udistrital.acciones.m8notificaciones.domain.EntregaCorreo;
import co.edu.udistrital.acciones.m8notificaciones.domain.EventoAviso;
import co.edu.udistrital.acciones.m8notificaciones.domain.Notificacion;
import co.edu.udistrital.acciones.m8notificaciones.domain.Preferencia;
import co.edu.udistrital.acciones.m8notificaciones.domain.TipoAviso;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AvisosRepositoryPort {
    boolean registrarEvento(EventoAviso evento);

    void finalizarEvento(UUID id);

    List<Notificacion> notificaciones(long userId, int offset, int limit, boolean unreadOnly);

    long total(long userId, boolean unreadOnly);

    boolean marcarLeida(long userId, long id);

    long crearNotificacion(
            UUID eventId,
            long userId,
            TipoAviso type,
            String title,
            String message,
            Long orderId,
            String market,
            boolean visible);

    boolean habilitada(long userId, TipoAviso type, Canal channel);

    void preferencia(long userId, Preferencia preference);

    void suscribir(long userId, String market);

    void desuscribir(long userId, String market);

    List<String> mercados(long userId);

    List<Long> suscriptores(String market, long afterUserId);

    AlertaPrecio crearAlerta(
            long userId,
            long instrumentId,
            String symbol,
            CondicionPrecio condition,
            BigDecimal target,
            String key,
            String hash);

    List<AlertaPrecio> alertas(long userId);

    List<AlertaPrecio> alertasActivas(long afterId);

    boolean desactivarAlerta(long userId, long id);

    void desactivarAlertas(long userId);

    boolean dispararAlerta(long id);

    long crearReporte(long userId, YearMonth period);

    void encolarCorreo(
            UUID id,
            long userId,
            Long notificationId,
            Long reportId,
            String encryptedContent,
            Instant expiresAt);

    Optional<EntregaCorreo> bloquearCorreo();

    void fijarContenidoCorreo(UUID id, String encryptedContent);

    void correoEnviado(UUID id, String providerId);

    void correoFallido(UUID id, String state, String code, Instant nextAttempt);
}

package co.edu.udistrital.acciones.m8notificaciones.infrastructure.adapter.out;

import co.edu.udistrital.acciones.m8notificaciones.application.port.out.AvisosRepositoryPort;
import co.edu.udistrital.acciones.m8notificaciones.domain.AlertaPrecio;
import co.edu.udistrital.acciones.m8notificaciones.domain.AvisoException;
import co.edu.udistrital.acciones.m8notificaciones.domain.Canal;
import co.edu.udistrital.acciones.m8notificaciones.domain.CondicionPrecio;
import co.edu.udistrital.acciones.m8notificaciones.domain.EntregaCorreo;
import co.edu.udistrital.acciones.m8notificaciones.domain.EventoAviso;
import co.edu.udistrital.acciones.m8notificaciones.domain.Notificacion;
import co.edu.udistrital.acciones.m8notificaciones.domain.Preferencia;
import co.edu.udistrital.acciones.m8notificaciones.domain.TipoAviso;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcAvisosRepository implements AvisosRepositoryPort {
    private static final RowMapper<AlertaPrecio> ALERTA =
            (r, n) ->
                    new AlertaPrecio(
                            r.getLong("id"),
                            r.getLong("usuario_id"),
                            r.getLong("accion_id"),
                            r.getString("symbol"),
                            CondicionPrecio.valueOf(r.getString("condicion_alerta")),
                            r.getBigDecimal("precio_umbral"),
                            r.getBoolean("activa"),
                            instant(r, "creada_en"),
                            instant(r, "disparada_en"));
    private final JdbcTemplate jdbc;

    public JdbcAvisosRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean registrarEvento(EventoAviso event) {
        return jdbc.update(
                        "INSERT INTO inbox_evento(evento_id,servicio_origen,tipo_evento)"
                                + " VALUES(?,?,?) ON CONFLICT DO NOTHING",
                        event.eventId(),
                        event.source(),
                        event.eventType())
                == 1;
    }

    @Override
    public void finalizarEvento(UUID id) {
        jdbc.update("UPDATE inbox_evento SET procesado_en=CURRENT_TIMESTAMP WHERE evento_id=?", id);
    }

    @Override
    public List<Notificacion> notificaciones(long user, int offset, int limit, boolean unread) {
        return jdbc.query(
                "SELECT * FROM notificacion WHERE usuario_id=? AND visible_app AND (NOT ? OR"
                        + " leida_en IS NULL) ORDER BY creada_en DESC,id DESC LIMIT ? OFFSET ?",
                (r, n) ->
                        new Notificacion(
                                r.getLong("id"),
                                r.getLong("usuario_id"),
                                TipoAviso.valueOf(r.getString("tipo_evento")),
                                r.getString("titulo"),
                                r.getString("mensaje"),
                                r.getObject("orden_id", Long.class),
                                r.getString("mercado_codigo"),
                                instant(r, "creada_en"),
                                instant(r, "leida_en")),
                user,
                unread,
                limit,
                offset);
    }

    @Override
    public long total(long user, boolean unread) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM notificacion WHERE usuario_id=? AND visible_app AND (NOT ? OR"
                        + " leida_en IS NULL)",
                Long.class,
                user,
                unread);
    }

    @Override
    public boolean marcarLeida(long user, long id) {
        return jdbc.update(
                        "UPDATE notificacion SET leida_en=COALESCE(leida_en,CURRENT_TIMESTAMP)"
                                + " WHERE id=? AND usuario_id=? AND visible_app",
                        id,
                        user)
                == 1;
    }

    @Override
    public long crearNotificacion(
            UUID eventId,
            long user,
            TipoAviso type,
            String title,
            String message,
            Long order,
            String market,
            boolean visible) {
        return jdbc.queryForObject(
                "INSERT INTO"
                    + " notificacion(evento_id,usuario_id,tipo_evento,titulo,mensaje,orden_id,mercado_codigo,visible_app)"
                    + " VALUES(?,?,?,?,?,?,?,?) RETURNING id",
                Long.class,
                eventId,
                user,
                type.name(),
                title,
                message,
                order,
                market,
                visible);
    }

    @Override
    public boolean habilitada(long user, TipoAviso type, Canal channel) {
        List<Boolean> values =
                jdbc.query(
                        "SELECT habilitada FROM preferencia_notificacion WHERE usuario_id=? AND"
                                + " tipo_evento=? AND canal=?",
                        (r, n) -> r.getBoolean(1),
                        user,
                        type.name(),
                        channel.name());
        return values.isEmpty() || values.getFirst();
    }

    @Override
    public void preferencia(long user, Preferencia preference) {
        jdbc.update(
                "INSERT INTO preferencia_notificacion(usuario_id,tipo_evento,canal,habilitada)"
                        + " VALUES(?,?,?,?) ON CONFLICT(usuario_id,tipo_evento,canal) DO UPDATE SET"
                        + " habilitada=EXCLUDED.habilitada,actualizada_en=CURRENT_TIMESTAMP",
                user,
                preference.type().name(),
                preference.channel().name(),
                preference.enabled());
    }

    @Override
    public void suscribir(long user, String market) {
        jdbc.update(
                "INSERT INTO suscripcion_aviso_mercado(usuario_id,mercado_codigo) VALUES(?,?) ON"
                        + " CONFLICT DO NOTHING",
                user,
                market);
    }

    @Override
    public void desuscribir(long user, String market) {
        jdbc.update(
                "DELETE FROM suscripcion_aviso_mercado WHERE usuario_id=? AND mercado_codigo=?",
                user,
                market);
    }

    @Override
    public List<String> mercados(long user) {
        return jdbc.query(
                "SELECT mercado_codigo FROM suscripcion_aviso_mercado WHERE usuario_id=? ORDER BY"
                        + " mercado_codigo",
                (r, n) -> r.getString(1),
                user);
    }

    @Override
    public List<Long> suscriptores(String market, long afterUser) {
        return jdbc.query(
                "SELECT usuario_id FROM suscripcion_aviso_mercado WHERE mercado_codigo=? AND"
                        + " usuario_id>? ORDER BY usuario_id LIMIT 200",
                (r, n) -> r.getLong(1),
                market,
                afterUser);
    }

    @Override
    public AlertaPrecio crearAlerta(
            long user,
            long instrument,
            String symbol,
            CondicionPrecio condition,
            BigDecimal target,
            String key,
            String hash) {
        jdbc.update(
                "INSERT INTO"
                    + " alerta_precio(usuario_id,accion_id,symbol,condicion_alerta,precio_umbral,clave_idempotencia,solicitud_hash)"
                    + " VALUES(?,?,?,?,?,?,?) ON CONFLICT(usuario_id,clave_idempotencia) DO"
                    + " NOTHING",
                user,
                instrument,
                symbol,
                condition.name(),
                target,
                key,
                hash);
        String existingHash =
                jdbc.queryForObject(
                        "SELECT solicitud_hash FROM alerta_precio WHERE usuario_id=? AND"
                                + " clave_idempotencia=?",
                        String.class,
                        user,
                        key);
        if (!hash.equals(existingHash)) {
            throw new AvisoException(
                    409, "La clave de idempotencia ya se utilizó con otros datos.");
        }
        return jdbc.queryForObject(
                "SELECT * FROM alerta_precio WHERE usuario_id=? AND clave_idempotencia=?",
                ALERTA,
                user,
                key);
    }

    @Override
    public List<AlertaPrecio> alertas(long user) {
        return jdbc.query(
                "SELECT * FROM alerta_precio WHERE usuario_id=? ORDER BY id DESC LIMIT 1000",
                ALERTA,
                user);
    }

    @Override
    public List<AlertaPrecio> alertasActivas(long afterId) {
        return jdbc.query(
                "SELECT * FROM alerta_precio WHERE activa AND id>? ORDER BY id LIMIT 200",
                ALERTA,
                afterId);
    }

    @Override
    public boolean desactivarAlerta(long user, long id) {
        return jdbc.update(
                        "UPDATE alerta_precio SET activa=FALSE WHERE usuario_id=? AND id=?",
                        user,
                        id)
                == 1;
    }

    @Override
    public void desactivarAlertas(long user) {
        jdbc.update("UPDATE alerta_precio SET activa=FALSE WHERE usuario_id=? AND activa", user);
    }

    @Override
    public boolean dispararAlerta(long id) {
        return jdbc.update(
                        "UPDATE alerta_precio SET activa=FALSE,disparada_en=CURRENT_TIMESTAMP WHERE"
                                + " id=? AND activa",
                        id)
                == 1;
    }

    @Override
    public long crearReporte(long user, YearMonth period) {
        List<Long> ids =
                jdbc.query(
                        "INSERT INTO reporte_mensual(usuario_id,periodo) VALUES(?,?) ON"
                                + " CONFLICT(usuario_id,periodo) DO NOTHING RETURNING id",
                        (r, n) -> r.getLong(1),
                        user,
                        java.sql.Date.valueOf(period.atDay(1)));
        return ids.isEmpty() ? 0 : ids.getFirst();
    }

    @Override
    public void encolarCorreo(
            UUID id, long user, Long notification, Long report, String encrypted, Instant expires) {
        jdbc.update(
                "INSERT INTO"
                    + " entrega_correo(id,usuario_id,notificacion_id,reporte_id,contenido_cifrado,expira_en)"
                    + " VALUES(?,?,?,?,?,?) ON CONFLICT DO NOTHING",
                id,
                user,
                notification,
                report,
                encrypted,
                Timestamp.from(expires));
    }

    @Override
    public Optional<EntregaCorreo> bloquearCorreo() {
        return jdbc
                .query(
                        "SELECT id,contenido_cifrado,intentos,expira_en FROM entrega_correo WHERE"
                            + " estado='PENDIENTE' AND proximo_intento_en<=CURRENT_TIMESTAMP ORDER"
                            + " BY proximo_intento_en,id LIMIT 1 FOR UPDATE SKIP LOCKED",
                        (r, n) ->
                                new EntregaCorreo(
                                        r.getObject("id", UUID.class),
                                        r.getString("contenido_cifrado"),
                                        r.getInt("intentos"),
                                        instant(r, "expira_en")))
                .stream()
                .findFirst();
    }

    @Override
    public void fijarContenidoCorreo(UUID id, String encrypted) {
        jdbc.update("UPDATE entrega_correo SET contenido_cifrado=? WHERE id=?", encrypted, id);
    }

    @Override
    public void correoEnviado(UUID id, String providerId) {
        jdbc.update(
                "UPDATE entrega_correo SET"
                    + " estado='ENVIADO',intentos=intentos+1,enviada_en=CURRENT_TIMESTAMP,proveedor_id=?,contenido_cifrado='',error_codigo=NULL"
                    + " WHERE id=?",
                providerId,
                id);
        jdbc.update(
                "UPDATE notificacion SET correo_enviado_en=CURRENT_TIMESTAMP WHERE id=(SELECT"
                        + " notificacion_id FROM entrega_correo WHERE id=?)",
                id);
        jdbc.update(
                "UPDATE reporte_mensual SET enviado_en=CURRENT_TIMESTAMP WHERE id=(SELECT"
                        + " reporte_id FROM entrega_correo WHERE id=?)",
                id);
    }

    @Override
    public void correoFallido(UUID id, String state, String code, Instant next) {
        jdbc.update(
                "UPDATE entrega_correo SET"
                    + " estado=?,intentos=intentos+1,error_codigo=?,proximo_intento_en=?,contenido_cifrado=CASE"
                    + " WHEN ?='PENDIENTE' THEN contenido_cifrado ELSE '' END WHERE id=?",
                state,
                code,
                Timestamp.from(next),
                state,
                id);
    }

    private static Instant instant(ResultSet result, String column) throws SQLException {
        Timestamp value = result.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}

package co.edu.udistrital.acciones.m1identidad.infrastructure.adapter.out;

import co.edu.udistrital.acciones.m1identidad.application.port.out.PasswordResetCompletionPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;

/** Joins the password update transaction; it never publishes before the database commit. */
@Component
public class PasswordResetCompletionOutbox implements PasswordResetCompletionPort {
    public static final String EVENT_TYPE = "m1.recuperacion.completada";
    private final JdbcTemplate sql;
    private final ObjectMapper json;

    public PasswordResetCompletionOutbox(JdbcTemplate sql, ObjectMapper json) {
        this.sql = sql;
        this.json = json;
    }

    @Override
    public void register(Long userId, String email) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        String envelope;
        try {
            envelope = json.writeValueAsString(Map.of("eventId", id.toString(), "eventType", EVENT_TYPE,
                    "source", "M1", "occurredAt", now.toString(), "payload", Map.of("userId", userId, "email", email)));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudo preparar el aviso de cambio de contraseña");
        }
        sql.update("""
                insert into outbox_evento(id,tipo_agregado,agregado_id,tipo_evento,carga,creado_en,intentos)
                values (?, 'User', ?, ?, cast(? as jsonb), ?, 0)
                """, id, userId.toString(), EVENT_TYPE, envelope, Timestamp.from(now));
    }
}

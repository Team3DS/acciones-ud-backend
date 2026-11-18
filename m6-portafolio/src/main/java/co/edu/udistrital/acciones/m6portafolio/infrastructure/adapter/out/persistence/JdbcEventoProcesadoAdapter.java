package co.edu.udistrital.acciones.m6portafolio.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m6portafolio.application.port.out.EventoProcesadoPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Repository
public class JdbcEventoProcesadoAdapter implements EventoProcesadoPort {

    private static final String SQL = """
            INSERT INTO evento_procesado (event_id, event_type, procesado_en)
            VALUES (?, ?, ?)
            ON CONFLICT (event_id) DO NOTHING
            """;

    private final JdbcTemplate jdbc;

    public JdbcEventoProcesadoAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean registrarSiNuevo(String eventId, String eventType, Instant procesadoEn) {
        OffsetDateTime instante = OffsetDateTime.ofInstant(procesadoEn, ZoneOffset.UTC);
        return jdbc.update(SQL, eventId, eventType, instante) > 0;
    }
}

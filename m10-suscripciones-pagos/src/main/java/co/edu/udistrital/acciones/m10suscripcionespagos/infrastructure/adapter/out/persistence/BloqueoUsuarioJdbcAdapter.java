package co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.BloqueoUsuarioPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BloqueoUsuarioJdbcAdapter implements BloqueoUsuarioPort {

    private final JdbcTemplate jdbc;

    public BloqueoUsuarioJdbcAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void bloquear(Long userId) {
        jdbc.query("SELECT pg_advisory_xact_lock(?)", resultado -> {
        }, userId);
    }
}

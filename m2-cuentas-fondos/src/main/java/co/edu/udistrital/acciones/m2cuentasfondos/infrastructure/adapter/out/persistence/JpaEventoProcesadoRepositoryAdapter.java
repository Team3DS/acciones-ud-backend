package co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.EventoProcesadoPort;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.EventoProcesado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
interface SpringDataEventoProcesadoRepository extends JpaRepository<EventoProcesado, String> {

    @Modifying
    @Query(value = """
            INSERT INTO evento_procesado (event_id, event_type, procesado_en)
            VALUES (:eventId, :eventType, :procesadoEn)
            ON CONFLICT (event_id) DO NOTHING
            """, nativeQuery = true)
    int registrarSiNuevo(@Param("eventId") String eventId, @Param("eventType") String eventType,
                         @Param("procesadoEn") Instant procesadoEn);
}

@Repository
public class JpaEventoProcesadoRepositoryAdapter implements EventoProcesadoPort {

    private final SpringDataEventoProcesadoRepository repositorio;

    public JpaEventoProcesadoRepositoryAdapter(SpringDataEventoProcesadoRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public boolean registrarSiNuevo(String eventId, String eventType, Instant procesadoEn) {
        return repositorio.registrarSiNuevo(eventId, eventType, procesadoEn) > 0;
    }
}

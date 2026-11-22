package co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.EventoStripeRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.EventoStripe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
interface SpringDataEventoStripeRepository extends JpaRepository<EventoStripe, String> {

    @Modifying
    @Query(value = """
            INSERT INTO evento_stripe (id, tipo, objeto_id, payload, recibido_en, procesado_en,
                                       intentos, ultimo_error, siguiente_intento_en)
            VALUES (:id, :tipo, :objetoId, :payload, :recibidoEn, NULL, 0, NULL, :recibidoEn)
            ON CONFLICT (id) DO NOTHING
            """, nativeQuery = true)
    int registrarSiNuevo(@Param("id") String id, @Param("tipo") String tipo,
                         @Param("objetoId") String objetoId, @Param("payload") String payload,
                         @Param("recibidoEn") Instant recibidoEn);

    @Query(value = """
            SELECT id FROM evento_stripe
            WHERE procesado_en IS NULL
              AND intentos < :maxIntentos
              AND siguiente_intento_en <= :ahora
            ORDER BY siguiente_intento_en, recibido_en, id
            LIMIT :limite
            """, nativeQuery = true)
    List<String> findPendientes(@Param("ahora") Instant ahora,
                                @Param("maxIntentos") int maxIntentos,
                                @Param("limite") int limite);

    @Query(value = """
            SELECT * FROM evento_stripe
            WHERE id = :id
              AND procesado_en IS NULL
              AND intentos < :maxIntentos
              AND siguiente_intento_en <= :ahora
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<EventoStripe> bloquearParaProcesar(@Param("id") String id,
                                                @Param("ahora") Instant ahora,
                                                @Param("maxIntentos") int maxIntentos);
}

@Repository
public class JpaEventoStripeRepositoryAdapter implements EventoStripeRepositoryPort {

    private final SpringDataEventoStripeRepository repositorio;

    public JpaEventoStripeRepositoryAdapter(SpringDataEventoStripeRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public boolean registrarSiNuevo(EventoStripe evento) {
        return repositorio.registrarSiNuevo(evento.getId(), evento.getTipo(), evento.getObjetoId(),
                evento.getPayload(), evento.getRecibidoEn()) > 0;
    }

    @Override
    public List<String> findPendientes(Instant ahora, int maxIntentos, int limite) {
        return repositorio.findPendientes(ahora, maxIntentos, limite);
    }

    @Override
    public Optional<EventoStripe> bloquearParaProcesar(String id, Instant ahora, int maxIntentos) {
        return repositorio.bloquearParaProcesar(id, ahora, maxIntentos);
    }

    @Override
    public Optional<EventoStripe> findById(String id) {
        return repositorio.findById(id);
    }

    @Override
    public EventoStripe save(EventoStripe evento) {
        return repositorio.save(evento);
    }
}

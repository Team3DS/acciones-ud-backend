package co.edu.udistrital.acciones.m6portafolio.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m6portafolio.domain.EjecucionAplicada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaEjecucionAplicadaRepository extends JpaRepository<EjecucionAplicada, Long> {
    Optional<EjecucionAplicada> findByClaveIdempotencia(UUID claveIdempotencia);
    Optional<EjecucionAplicada> findByEjecucionOrdenId(String ejecucionOrdenId);
    boolean existsByClaveIdempotencia(UUID claveIdempotencia);
    boolean existsByEjecucionOrdenId(String ejecucionOrdenId);
}

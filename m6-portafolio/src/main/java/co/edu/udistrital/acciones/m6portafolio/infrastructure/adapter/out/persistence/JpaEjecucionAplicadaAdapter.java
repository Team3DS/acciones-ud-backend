package co.edu.udistrital.acciones.m6portafolio.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m6portafolio.application.port.out.EjecucionAplicadaPort;
import co.edu.udistrital.acciones.m6portafolio.domain.EjecucionAplicada;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaEjecucionAplicadaAdapter implements EjecucionAplicadaPort {

    private final JpaEjecucionAplicadaRepository repositorio;

    public JpaEjecucionAplicadaAdapter(JpaEjecucionAplicadaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public boolean existePorClaveIdempotencia(UUID claveIdempotencia) {
        return repositorio.existsByClaveIdempotencia(claveIdempotencia);
    }

    @Override
    public Optional<EjecucionAplicada> findByClaveIdempotencia(UUID claveIdempotencia) {
        return repositorio.findByClaveIdempotencia(claveIdempotencia);
    }

    @Override
    public EjecucionAplicada save(EjecucionAplicada ejecucion) {
        return repositorio.save(ejecucion);
    }
}
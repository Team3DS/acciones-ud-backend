package co.edu.udistrital.acciones.m6portafolio.application.port.out;

import co.edu.udistrital.acciones.m6portafolio.domain.EjecucionAplicada;
import co.edu.udistrital.acciones.m6portafolio.domain.LadoEjecucion;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface EjecucionAplicadaPort {

    boolean existePorClaveIdempotencia(UUID claveIdempotencia);

    boolean existePorExecutionId(String executionId);

    Optional<EjecucionAplicada> findByClaveIdempotencia(UUID claveIdempotencia);

    EjecucionAplicada save(EjecucionAplicada ejecucion);
}
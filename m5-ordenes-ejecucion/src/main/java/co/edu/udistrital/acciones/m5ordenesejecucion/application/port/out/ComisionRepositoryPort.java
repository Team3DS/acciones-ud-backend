package co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out;

import co.edu.udistrital.acciones.m5ordenesejecucion.domain.ConfiguracionComision;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ComisionRepositoryPort {
    Optional<ConfiguracionComision> findVigentePorMercado(String mercado, Instant ahora);
    ConfiguracionComision save(ConfiguracionComision configuracion);
    List<ConfiguracionComision> findByMercado(String mercado);
}

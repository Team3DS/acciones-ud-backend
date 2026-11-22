package co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out;

import co.edu.udistrital.acciones.m5ordenesejecucion.domain.ConfiguracionComision;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ConfiguracionComisionRepositoryPort {
    Optional<ConfiguracionComision> findById(Long id);
    Optional<ConfiguracionComision> findVigentePorMercado(String mercadoCodigo, Instant momento);
    List<ConfiguracionComision> findVigentes(Instant momento);
    ConfiguracionComision save(ConfiguracionComision config);
}

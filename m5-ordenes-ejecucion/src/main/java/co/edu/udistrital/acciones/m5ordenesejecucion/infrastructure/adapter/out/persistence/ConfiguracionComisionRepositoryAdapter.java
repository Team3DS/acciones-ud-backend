package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.ConfiguracionComisionRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.ConfiguracionComision;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class ConfiguracionComisionRepositoryAdapter implements ConfiguracionComisionRepositoryPort {

    private final ConfiguracionComisionJpaRepository jpaRepository;

    public ConfiguracionComisionRepositoryAdapter(ConfiguracionComisionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<ConfiguracionComision> findById(Long id) {
        return jpaRepository.findById(id).map(ConfiguracionComisionEntity::toDomain);
    }

    @Override
    public Optional<ConfiguracionComision> findVigentePorMercado(String mercadoCodigo, Instant momento) {
        List<ConfiguracionComisionEntity> list = jpaRepository.findVigentesPorMercado(mercadoCodigo, momento);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0).toDomain());
    }

    @Override
    public List<ConfiguracionComision> findVigentes(Instant momento) {
        return jpaRepository.findTodasVigentes(momento).stream()
                .map(ConfiguracionComisionEntity::toDomain)
                .toList();
    }

    @Override
    public ConfiguracionComision save(ConfiguracionComision config) {
        ConfiguracionComisionEntity entity = ConfiguracionComisionEntity.fromDomain(config);
        return jpaRepository.save(entity).toDomain();
    }
}

package co.edu.udistrital.acciones.m5ordenesejecucion.application.service;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.ConfigurarComisionUseCase;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.ConfiguracionComisionRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.ConfiguracionComision;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ConfigurarComisionUseCaseImpl implements ConfigurarComisionUseCase {

    private final ConfiguracionComisionRepositoryPort comisionRepository;

    public ConfigurarComisionUseCaseImpl(ConfiguracionComisionRepositoryPort comisionRepository) {
        this.comisionRepository = comisionRepository;
    }

    @Override
    public ConfiguracionComision configurar(ConfigurarComisionCommand comando) {
        if (comando.porcentaje() == null || comando.porcentaje().compareTo(BigDecimal.ZERO) < 0
                || comando.porcentaje().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("El porcentaje de comisión debe estar entre 0 y 100");
        }

        Instant ahora = Instant.now();
        String mercado = comando.mercadoCodigo().toUpperCase();

        // Cerrar vigencia de la configuración previa si existe
        Optional<ConfiguracionComision> anterior = comisionRepository.findVigentePorMercado(mercado, ahora);
        anterior.ifPresent(cfg -> {
            cfg.setVigenteHasta(ahora);
            comisionRepository.save(cfg);
        });

        ConfiguracionComision nueva = new ConfiguracionComision(
                null,
                mercado,
                comando.porcentaje(),
                ahora,
                null,
                comando.motivo() != null ? comando.motivo() : "Actualización de comisión",
                comando.adminUsuarioId(),
                ahora
        );

        return comisionRepository.save(nueva);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConfiguracionComision> listarVigentes() {
        return comisionRepository.findVigentes(Instant.now());
    }
}

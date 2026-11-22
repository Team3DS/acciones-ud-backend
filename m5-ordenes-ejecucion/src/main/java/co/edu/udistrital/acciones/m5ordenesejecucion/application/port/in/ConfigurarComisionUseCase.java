package co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in;

import co.edu.udistrital.acciones.m5ordenesejecucion.domain.ConfiguracionComision;

import java.math.BigDecimal;
import java.util.List;

public interface ConfigurarComisionUseCase {
    ConfiguracionComision configurar(ConfigurarComisionCommand comando);
    List<ConfiguracionComision> listarVigentes();

    record ConfigurarComisionCommand(
            String mercadoCodigo,
            BigDecimal porcentaje,
            String motivo,
            Long adminUsuarioId
    ) {}
}

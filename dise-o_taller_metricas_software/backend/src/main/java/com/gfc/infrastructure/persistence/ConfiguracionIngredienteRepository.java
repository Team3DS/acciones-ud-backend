package com.gfc.infrastructure.persistence;

import com.gfc.domain.model.ConfiguracionIngrediente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracionIngredienteRepository extends JpaRepository<ConfiguracionIngrediente, Long> {
    boolean existsByIngredienteId(Long ingredienteId);
}

package com.gfc.infrastructure.persistence;

import com.gfc.domain.model.MovimientoInventario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {
    boolean existsByIngredienteId(Long ingredienteId);
}

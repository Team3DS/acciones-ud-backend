package com.gfc.infrastructure.persistence;

import com.gfc.domain.model.Ingrediente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredienteRepository extends JpaRepository<Ingrediente, Long> {
    boolean existsByNombreIngredienteIgnoreCase(String nombre);

    boolean existsByNombreIngredienteIgnoreCaseAndIdNot(String nombre, Long id);
}

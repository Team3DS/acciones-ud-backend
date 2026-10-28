package com.gfc.infrastructure.persistence;

import com.gfc.domain.model.Comida;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComidaRepository extends JpaRepository<Comida, Long> {
}

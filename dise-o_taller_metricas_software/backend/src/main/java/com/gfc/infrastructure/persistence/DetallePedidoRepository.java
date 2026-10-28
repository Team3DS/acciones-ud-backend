package com.gfc.infrastructure.persistence;

import com.gfc.domain.model.DetallePedido;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetallePedidoRepository extends JpaRepository<DetallePedido, Long> {
    boolean existsByComidaId(Long comidaId);
}

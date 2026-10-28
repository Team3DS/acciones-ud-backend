package com.gfc.infrastructure.persistence;

import com.gfc.domain.model.Pedido;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
	List<Pedido> findByClienteNombreUsuarioOrderByFechaDesc(String nombreUsuario);
}

package com.gfc.infrastructure.persistence;

import com.gfc.domain.model.Cliente;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByNombreUsuario(String nombreUsuario);
    Optional<Cliente> findByEmail(String email);
    boolean existsByDocumento(String documento);
    boolean existsByEmail(String email);
}

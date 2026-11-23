package co.edu.udistrital.acciones.m1identidad.infrastructure.adapter.out;

import co.edu.udistrital.acciones.m1identidad.domain.UsuarioRol;
import co.edu.udistrital.acciones.m1identidad.domain.UsuarioRolId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsuarioRolJpaRepository extends JpaRepository<UsuarioRol, UsuarioRolId> {

    @Query("select r.id.rol from UsuarioRol r where r.id.usuarioId = :usuarioId")
    List<String> findRolesByUsuarioId(@Param("usuarioId") Long usuarioId);
}

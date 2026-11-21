package co.edu.udistrital.acciones.m6portafolio.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m6portafolio.domain.InboxEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaInboxEventoRepository extends JpaRepository<InboxEvento, String> {
}
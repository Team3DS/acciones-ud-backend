package co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out;

import co.edu.udistrital.acciones.m5ordenesejecucion.domain.OutboxEvento;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEventoRepositoryPort {
    Optional<OutboxEvento> findById(UUID id);
    List<OutboxEvento> findPendientes(int limite);
    OutboxEvento save(OutboxEvento evento);
}

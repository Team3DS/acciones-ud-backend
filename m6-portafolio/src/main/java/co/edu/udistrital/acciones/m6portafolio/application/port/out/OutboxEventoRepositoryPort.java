package co.edu.udistrital.acciones.m6portafolio.application.port.out;

import co.edu.udistrital.acciones.m6portafolio.domain.OutboxEvento;

import java.util.List;

public interface OutboxEventoRepositoryPort {

    OutboxEvento guardar(OutboxEvento evento);

    List<OutboxEvento> buscarPendientes(int lote);
}

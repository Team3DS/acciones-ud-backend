package co.edu.udistrital.acciones.m2cuentasfondos.application.port.out;

import co.edu.udistrital.acciones.m2cuentasfondos.domain.OutboxEvento;

import java.util.List;

public interface OutboxEventoRepositoryPort {

    OutboxEvento save(OutboxEvento evento);

    List<OutboxEvento> buscarPendientes(int lote);
}

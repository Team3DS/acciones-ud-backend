package co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out;

import co.edu.udistrital.acciones.m10suscripcionespagos.domain.OutboxEvento;

import java.util.List;

public interface OutboxEventoRepositoryPort {

    OutboxEvento guardar(OutboxEvento evento);

    List<OutboxEvento> buscarPendientes(int lote);
}

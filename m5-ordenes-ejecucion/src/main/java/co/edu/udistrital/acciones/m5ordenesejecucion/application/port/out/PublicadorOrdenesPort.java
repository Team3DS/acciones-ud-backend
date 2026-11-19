package co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out;

import co.edu.udistrital.acciones.m5ordenesejecucion.domain.Fill;

import java.math.BigDecimal;
import java.util.List;

public interface PublicadorOrdenesPort {
    void publicarOrdenCreada(Long ordenId, Long usuarioId, String simbolo, String lado, Integer cantidad, String tipo);
    void publicarOrdenEjecutada(Long ordenId, String executionId, List<Fill> fills, BigDecimal totalAmount);
    void publicarOrdenCancelada(Long ordenId, String motivo);
    void publicarOrdenRechazada(Long ordenId, String motivo, String brokerError);
}

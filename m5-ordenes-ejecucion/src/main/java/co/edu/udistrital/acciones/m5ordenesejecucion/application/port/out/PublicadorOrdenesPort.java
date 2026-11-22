package co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface PublicadorOrdenesPort {
    void publicarOrdenCreada(UUID eventId, Long ordenId, Long usuarioId, String symbol, String side, Integer quantity, String type);
    void publicarOrdenEjecutada(UUID eventId, String executionId, Long ordenId, Long userId, String symbol,
                                 String side, Integer quantity, BigDecimal price, BigDecimal commission, Instant executedAt);
    void publicarOrdenCancelada(UUID eventId, Long ordenId, Long usuarioId, String motivo);
    void publicarOrdenRechazada(UUID eventId, Long ordenId, Long usuarioId, String motivo, String brokerError);
}

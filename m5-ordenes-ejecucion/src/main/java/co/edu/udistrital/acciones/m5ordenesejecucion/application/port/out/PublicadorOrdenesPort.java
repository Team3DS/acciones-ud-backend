package co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface PublicadorOrdenesPort {
    void publicarOrdenCreada(Long ordenId, Long usuarioId, String symbol, String side, Integer quantity, String type);
    void publicarOrdenEjecutada(UUID eventId, Long executionId, Long ordenId, Long userId, String symbol,
                                 String side, Integer quantity, BigDecimal price, BigDecimal commission, Instant executedAt);
    void publicarOrdenCancelada(Long ordenId, String motivo);
    void publicarOrdenRechazada(Long ordenId, String motivo, String brokerError);
}

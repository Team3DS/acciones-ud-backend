package co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out;

import java.math.BigDecimal;

public interface FondosClientPort {
    Long reservarFondos(String orderId, Long userId, BigDecimal amount, String idempotencyKey);
    void confirmarReserva(Long reservationId, Long userId, String executionId, BigDecimal amount,
                          boolean finalExecution, Integer expectedExecutionCount);
    void liberarReserva(Long reservationId, Long userId, String reason);
}

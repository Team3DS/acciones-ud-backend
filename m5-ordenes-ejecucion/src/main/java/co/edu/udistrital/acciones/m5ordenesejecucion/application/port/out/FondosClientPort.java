package co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out;

import java.math.BigDecimal;

public interface FondosClientPort {
    Long reservarFondos(String orderId, Long userId, BigDecimal amount, String idempotencyKey);
    void confirmarReserva(Long reservationId, String executionId, BigDecimal amount);
    void liberarReserva(Long reservationId, String reason);
}

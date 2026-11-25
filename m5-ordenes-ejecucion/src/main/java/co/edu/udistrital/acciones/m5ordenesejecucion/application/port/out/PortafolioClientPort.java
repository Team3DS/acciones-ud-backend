package co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out;

import co.edu.udistrital.acciones.m5ordenesejecucion.domain.LadoOrden;

import java.math.BigDecimal;
import java.time.Instant;

public interface PortafolioClientPort {
    Long reservarPosicion(String orderId, Long userId, String symbol, Integer quantity, String idempotencyKey);
    BigDecimal actualizarPosicion(String orderId, String executionId, Long userId, String symbol,
                                 LadoOrden side, Integer quantity, BigDecimal price, Instant executedAt, String eventId);
    void liberarReserva(Long reservationId, Long userId, String reason);
}

package co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out;

import co.edu.udistrital.acciones.m5ordenesejecucion.domain.LadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.TipoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.VigenciaOrden;

import java.math.BigDecimal;
import java.util.UUID;

public interface AlpacaOrderPort {
    /** Prefijo de los ids del modo de simulación local sin claves de Alpaca. */
    String PREFIJO_SIMULADO = "alpaca-paper-";
    /** Referencia interna de un envío sin confirmación; se consulta por client_order_id, nunca se simula ni se reenvía. */
    String PREFIJO_PENDIENTE = "alpaca-pending-client-";

    AlpacaOrderResponse enviarOrden(AlpacaOrderRequest request);
    /** Devuelve el estado terminal confirmado; si la cancelación sigue pendiente debe lanzar una excepción. */
    AlpacaOrderResponse cancelarOrden(String brokerOrderId);
    AlpacaOrderResponse consultarOrden(String brokerOrderId);

    record AlpacaOrderRequest(
            String symbol,
            Integer qty,
            LadoOrden side,
            TipoOrden type,
            VigenciaOrden timeInForce,
            BigDecimal limitPrice,
            BigDecimal stopPrice,
            UUID clientOrderId
    ) {}

    record AlpacaOrderResponse(
            String id,
            String clientOrderId,
            String status,
            String symbol,
            Integer qty,
            BigDecimal filledAvgPrice,
            Integer filledQty
    ) {}
}

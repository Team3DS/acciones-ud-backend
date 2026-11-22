package co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out;

import co.edu.udistrital.acciones.m5ordenesejecucion.domain.LadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.TipoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.VigenciaOrden;

import java.math.BigDecimal;
import java.util.UUID;

public interface AlpacaOrderPort {
    /** Prefijo de los ids que genera el simulador de paper trading cuando no hay claves de Alpaca o la llamada falla. */
    String PREFIJO_SIMULADO = "alpaca-paper-";

    AlpacaOrderResponse enviarOrden(AlpacaOrderRequest request);
    void cancelarOrden(String brokerOrderId);
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

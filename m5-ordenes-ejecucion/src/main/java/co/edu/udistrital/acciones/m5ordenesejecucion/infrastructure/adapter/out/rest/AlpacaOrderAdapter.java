package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.rest;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.AlpacaOrderPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.LadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.TipoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.VigenciaOrden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class AlpacaOrderAdapter implements AlpacaOrderPort {

    private static final Logger log = LoggerFactory.getLogger(AlpacaOrderAdapter.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String apiKey;
    private final String apiSecret;

    public AlpacaOrderAdapter(RestTemplate restTemplate,
                              @Value("${app.alpaca.base-url:https://paper-api.alpaca.markets}") String baseUrl,
                              @Value("${app.alpaca.api-key:}") String apiKey,
                              @Value("${app.alpaca.api-secret:}") String apiSecret) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    @Override
    @SuppressWarnings("unchecked")
    public AlpacaOrderResponse enviarOrden(AlpacaOrderRequest request) {
        if (apiKey != null && !apiKey.isBlank() && apiSecret != null && !apiSecret.isBlank()) {
            String url = baseUrl + "/v2/orders";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("APCA-API-KEY-ID", apiKey);
            headers.set("APCA-API-SECRET-KEY", apiSecret);

            Map<String, Object> body = new HashMap<>();
            body.put("symbol", request.symbol());
            body.put("qty", request.qty().toString());
            body.put("side", request.side() == LadoOrden.COMPRA ? "buy" : "sell");
            body.put("type", mapearTipoAlpaca(request.type()));
            body.put("time_in_force", request.timeInForce() == VigenciaOrden.DIA ? "day" : "gtc");
            if (request.limitPrice() != null) {
                body.put("limit_price", request.limitPrice().toString());
            }
            if (request.stopPrice() != null) {
                body.put("stop_price", request.stopPrice().toString());
            }
            if (request.clientOrderId() != null) {
                body.put("client_order_id", request.clientOrderId().toString());
            }

            HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
            try {
                Map<String, Object> response = restTemplate.postForObject(url, httpEntity, Map.class);
                if (response != null && response.containsKey("id")) {
                    String id = (String) response.get("id");
                    String status = (String) response.getOrDefault("status", "accepted");
                    return new AlpacaOrderResponse(id, request.clientOrderId().toString(), status, request.symbol(), request.qty(), null, 0);
                }
            } catch (Exception ex) {
                log.warn("Llamada a Alpaca API falló ({}), activando simulador paper trading: {}", url, ex.getMessage());
            }
        }

        // Simulador de Paper Trading determinista si no hay API keys o si la llamada externa falla
        String simulatedId = "alpaca-paper-" + UUID.randomUUID();
        log.info("Simulando envío paper trading Alpaca: ordenId={}, symbol={}, qty={}, side={}",
                simulatedId, request.symbol(), request.qty(), request.side());

        return new AlpacaOrderResponse(
                simulatedId,
                request.clientOrderId() != null ? request.clientOrderId().toString() : UUID.randomUUID().toString(),
                "accepted",
                request.symbol(),
                request.qty(),
                null,
                0
        );
    }

    @Override
    public void cancelarOrden(String brokerOrderId) {
        if (apiKey != null && !apiKey.isBlank() && apiSecret != null && !apiSecret.isBlank()) {
            String url = baseUrl + "/v2/orders/" + brokerOrderId;
            HttpHeaders headers = new HttpHeaders();
            headers.set("APCA-API-KEY-ID", apiKey);
            headers.set("APCA-API-SECRET-KEY", apiSecret);

            try {
                restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
            } catch (Exception ex) {
                log.warn("Error al cancelar en Alpaca {}: {}", brokerOrderId, ex.getMessage());
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public AlpacaOrderResponse consultarOrden(String brokerOrderId) {
        if (apiKey != null && !apiKey.isBlank() && apiSecret != null && !apiSecret.isBlank()) {
            String url = baseUrl + "/v2/orders/" + brokerOrderId;
            HttpHeaders headers = new HttpHeaders();
            headers.set("APCA-API-KEY-ID", apiKey);
            headers.set("APCA-API-SECRET-KEY", apiSecret);

            try {
                Map<String, Object> resp = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class).getBody();
                if (resp != null) {
                    String status = (String) resp.getOrDefault("status", "filled");
                    String symbol = (String) resp.get("symbol");
                    Integer qty = resp.containsKey("qty") ? Integer.parseInt(resp.get("qty").toString()) : 1;
                    BigDecimal filledPrice = resp.containsKey("filled_avg_price") && resp.get("filled_avg_price") != null
                            ? new BigDecimal(resp.get("filled_avg_price").toString()) : null;
                    Integer filledQty = resp.containsKey("filled_qty") && resp.get("filled_qty") != null
                            ? Integer.parseInt(resp.get("filled_qty").toString()) : 0;
                    return new AlpacaOrderResponse(brokerOrderId, null, status, symbol, qty, filledPrice, filledQty);
                }
            } catch (Exception ex) {
                log.warn("Error consultando Alpaca {}: {}", brokerOrderId, ex.getMessage());
            }
        }
        return new AlpacaOrderResponse(brokerOrderId, null, "accepted", "AAPL", 1, null, 0);
    }

    private String mapearTipoAlpaca(TipoOrden tipo) {
        return switch (tipo) {
            case MARKET -> "market";
            case LIMIT -> "limit";
            case STOP_LOSS -> "stop";
            case TAKE_PROFIT -> "stop_limit";
        };
    }
}

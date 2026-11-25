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
import org.springframework.web.client.HttpClientErrorException;

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
            UUID clientOrderId = request.clientOrderId() != null ? request.clientOrderId() : UUID.randomUUID();
            String url = baseUrl + "/v2/orders";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("APCA-API-KEY-ID", apiKey);
            headers.set("APCA-API-SECRET-KEY", apiSecret);

            Map<String, Object> body = new HashMap<>();
            body.put("symbol", request.symbol());
            body.put("qty", request.qty().toString());
            body.put("side", request.side() == LadoOrden.COMPRA ? "buy" : "sell");
            // Take profit sin precio límite: orden límite en el precio objetivo (stop_limit exige limit_price).
            boolean takeProfitSimple = request.type() == TipoOrden.TAKE_PROFIT && request.limitPrice() == null;
            body.put("type", takeProfitSimple ? "limit" : mapearTipoAlpaca(request.type()));
            body.put("time_in_force", request.timeInForce() == VigenciaOrden.DIA ? "day" : "gtc");
            if (request.limitPrice() != null) {
                body.put("limit_price", request.limitPrice().toString());
            }
            if (takeProfitSimple && request.stopPrice() != null) {
                body.put("limit_price", request.stopPrice().toString());
            } else if (request.stopPrice() != null) {
                body.put("stop_price", request.stopPrice().toString());
            }
            body.put("client_order_id", clientOrderId.toString());

            HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
            Map<String, Object> response;
            try {
                response = restTemplate.postForObject(url, httpEntity, Map.class);
            } catch (Exception ex) {
                log.warn("Llamada a Alpaca API falló ({}): {}", url, ex.getMessage());
                if (ex instanceof HttpClientErrorException rechazo && esRechazoDefinitivo(rechazo)) {
                    throw new IllegalStateException("Alpaca rechazó la solicitud de la orden", ex);
                }
                return pendiente(request, clientOrderId);
            }
            if (response == null || !(response.get("id") instanceof String id) || id.isBlank()) {
                return pendiente(request, clientOrderId);
            }
            String status = response.get("status") instanceof String recibido ? recibido : "accepted";
            return new AlpacaOrderResponse(id, clientOrderId.toString(), status, request.symbol(), request.qty(), null, 0);
        }
        if ((apiKey != null && !apiKey.isBlank()) || (apiSecret != null && !apiSecret.isBlank())) {
            throw new IllegalStateException("Las credenciales de Alpaca están incompletas");
        }

        // Modo local sin credenciales. Un fallo del broker configurado nunca se convierte en una ejecución simulada.
        String simulatedId = PREFIJO_SIMULADO + UUID.randomUUID();
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
    public AlpacaOrderResponse cancelarOrden(String brokerOrderId) {
        if (brokerOrderId != null && brokerOrderId.startsWith(PREFIJO_SIMULADO)) {
            return new AlpacaOrderResponse(brokerOrderId, null, "canceled", null, null, null, 0);
        }
        if (apiKey != null && !apiKey.isBlank() && apiSecret != null && !apiSecret.isBlank()) {
            AlpacaOrderResponse estado = consultarOrden(brokerOrderId);
            if (estado == null || estado.id() == null || estado.id().startsWith(PREFIJO_PENDIENTE)) {
                throw new IllegalStateException("El estado de la orden sigue sin confirmar; se mantienen sus reservas");
            }
            if (cancelacionTerminada(estado)) {
                return estado;
            }
            if ("filled".equals(estado.status())) {
                throw new IllegalStateException("Alpaca informa una ejecución pendiente de conciliar; se mantienen las reservas");
            }
            brokerOrderId = estado.id();
            String url = baseUrl + "/v2/orders/" + brokerOrderId;
            HttpHeaders headers = new HttpHeaders();
            headers.set("APCA-API-KEY-ID", apiKey);
            headers.set("APCA-API-SECRET-KEY", apiSecret);

            try {
                restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
            } catch (Exception ex) {
                log.warn("Error al cancelar en Alpaca {}: {}", brokerOrderId, ex.getMessage());
                throw new IllegalStateException("No se pudo cancelar la orden en Alpaca; se mantienen sus reservas", ex);
            }
            AlpacaOrderResponse confirmada = consultarOrden(brokerOrderId);
            if (!cancelacionTerminada(confirmada)) {
                throw new IllegalStateException("La cancelación todavía no está confirmada por Alpaca; se mantienen las reservas");
            }
            return confirmada;
        }
        throw new IllegalStateException("No hay credenciales de Alpaca para cancelar la orden; se mantienen sus reservas");
    }

    @Override
    @SuppressWarnings("unchecked")
    public AlpacaOrderResponse consultarOrden(String brokerOrderId) {
        if (apiKey != null && !apiKey.isBlank() && apiSecret != null && !apiSecret.isBlank()) {
            boolean pendiente = brokerOrderId != null && brokerOrderId.startsWith(PREFIJO_PENDIENTE);
            String url = pendiente
                    ? baseUrl + "/v2/orders:by_client_order_id?client_order_id="
                            + UUID.fromString(brokerOrderId.substring(PREFIJO_PENDIENTE.length()))
                    : baseUrl + "/v2/orders/" + brokerOrderId;
            HttpHeaders headers = new HttpHeaders();
            headers.set("APCA-API-KEY-ID", apiKey);
            headers.set("APCA-API-SECRET-KEY", apiSecret);

            try {
                Map<String, Object> resp = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class).getBody();
                if (resp != null) {
                    String id = resp.get("id") instanceof String recibido && !recibido.isBlank() ? recibido : null;
                    if (pendiente && id == null) {
                        return null;
                    }
                    String status = (String) resp.getOrDefault("status", "accepted");
                    String symbol = (String) resp.get("symbol");
                    Integer qty = resp.containsKey("qty") ? Integer.parseInt(resp.get("qty").toString()) : 1;
                    BigDecimal filledPrice = resp.containsKey("filled_avg_price") && resp.get("filled_avg_price") != null
                            ? new BigDecimal(resp.get("filled_avg_price").toString()) : null;
                    Integer filledQty = resp.containsKey("filled_qty") && resp.get("filled_qty") != null
                            ? Integer.parseInt(resp.get("filled_qty").toString()) : null;
                    String clientId = resp.get("client_order_id") instanceof String cliente ? cliente : null;
                    return new AlpacaOrderResponse(id != null ? id : brokerOrderId, clientId, status, symbol, qty, filledPrice, filledQty);
                }
            } catch (Exception ex) {
                log.warn("Error consultando Alpaca {}: {}", brokerOrderId, ex.getMessage());
            }
        }
        // Un 404 o un fallo temporal no demuestra rechazo: el sincronizador volverá a consultar sin reenviar ni liberar.
        return null;
    }

    private static boolean esRechazoDefinitivo(HttpClientErrorException rechazo) {
        int codigo = rechazo.getStatusCode().value();
        // Timeout, client_order_id ya existente o saturación necesitan consulta antes de decidir qué ocurrió.
        return codigo != 408 && codigo != 409 && codigo != 429;
    }

    private static boolean cancelacionTerminada(AlpacaOrderResponse estado) {
        return estado != null && estado.status() != null
                && java.util.Set.of("canceled", "expired", "rejected").contains(estado.status());
    }

    private static AlpacaOrderResponse pendiente(AlpacaOrderRequest request, UUID clientOrderId) {
        log.warn("Envío de Alpaca sin confirmar para client_order_id={}; se conservan las reservas y se consultará su estado",
                clientOrderId);
        return new AlpacaOrderResponse(PREFIJO_PENDIENTE + clientOrderId, clientOrderId.toString(),
                "submission_pending", request.symbol(), request.qty(), null, 0);
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

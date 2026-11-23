package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.rest;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.DatosMercadoClientPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.config.RestClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class RestDatosMercadoClient implements DatosMercadoClientPort {

    private static final Logger log = LoggerFactory.getLogger(RestDatosMercadoClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    // M4 exige token: el cliente interno agrega el de sistema.
    public RestDatosMercadoClient(@Qualifier(RestClientConfig.REST_TEMPLATE_INTERNO) RestTemplate restTemplate,
                                  @Value("${app.m4-client.base-url:http://m4-datos-mercado:8084}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean esMercadoAbierto(String market) {
        String url = baseUrl + "/api/v1/market/status?market=" + market;
        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("estado")) {
                String estado = (String) response.get("estado");
                return "ABIERTO".equalsIgnoreCase(estado);
            }
        } catch (Exception ex) {
            log.warn("No se pudo consultar M4 para estado del mercado ({}), asumiendo ABIERTO para paper trading: {}", url, ex.getMessage());
        }
        return true; // Default fallback para desarrollo/pruebas si M4 no responde
    }

    @Override
    @SuppressWarnings("unchecked")
    public BigDecimal obtenerPrecioEstimado(String symbol) {
        String url = baseUrl + "/api/v1/quotes/" + symbol;
        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("price")) {
                Object p = response.get("price");
                return new BigDecimal(p.toString());
            }
        } catch (Exception ex) {
            log.warn("No se pudo consultar cotización en M4 para {}, usando precio por defecto 150.00: {}", symbol, ex.getMessage());
        }
        return new BigDecimal("150.00");
    }
}

package co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.config;

import org.slf4j.MDC;
import org.springframework.web.client.RestClient;

public final class CabecerasServicio {

    public static final String CORRELATION_ID = "X-Correlation-ID";

    private CabecerasServicio() {
    }

    public static RestClient.RequestHeadersSpec<?> aplicar(RestClient.RequestHeadersSpec<?> peticion,
                                                           String token) {
        return peticion.headers(cabeceras -> {
            cabeceras.setBearerAuth(token);
            String correlationId = MDC.get(CorrelationIdFilter.CLAVE_MDC);
            if (correlationId != null && !correlationId.isBlank()) {
                cabeceras.set(CORRELATION_ID, correlationId);
            }
        });
    }
}

package co.edu.udistrital.acciones.m6portafolio.infrastructure.adapter.out.mercado;

import co.edu.udistrital.acciones.m6portafolio.application.dto.CotizacionMercado;
import co.edu.udistrital.acciones.m6portafolio.application.dto.EstadoMercado;
import co.edu.udistrital.acciones.m6portafolio.application.exception.DatosMercadoNoDisponibleException;
import co.edu.udistrital.acciones.m6portafolio.application.port.out.DatosMercadoClientPort;
import co.edu.udistrital.acciones.m6portafolio.domain.Simbolos;
import co.edu.udistrital.acciones.m6portafolio.infrastructure.config.CorrelationIdFilter;
import co.edu.udistrital.acciones.m6portafolio.infrastructure.security.TokenServicioProvider;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.MDC;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class DatosMercadoRestClient implements DatosMercadoClientPort {

    private static final String CORRELATION_ID = "X-Correlation-ID";
    private static final ParameterizedTypeReference<List<RespuestaCotizacion>> LISTA_COTIZACIONES =
            new ParameterizedTypeReference<>() { };

    private final RestClient cliente;
    private final TokenServicioProvider tokenServicio;
    private final CacheCotizaciones cache;

    public DatosMercadoRestClient(RestClient m4RestClient, TokenServicioProvider tokenServicio,
                                  CacheCotizaciones cache) {
        this.cliente = m4RestClient;
        this.tokenServicio = tokenServicio;
        this.cache = cache;
    }

    @Override
    public Map<String, CotizacionMercado> obtenerCotizaciones(Set<String> symbols) {
        Map<String, CotizacionMercado> resultado = new LinkedHashMap<>();
        Set<String> faltantes = new LinkedHashSet<>();
        for (String original : symbols) {
            String symbol = Simbolos.normalizar(original);
            cache.obtener(symbol).ifPresentOrElse(cotizacion -> resultado.put(symbol, cotizacion),
                    () -> faltantes.add(symbol));
        }
        if (faltantes.isEmpty()) {
            return Map.copyOf(resultado);
        }
        try {
            convertir(obtenerLote(faltantes)).forEach((symbol, cotizacion) -> {
                cache.guardar(cotizacion);
                resultado.put(symbol, cotizacion);
            });
        } catch (RestClientResponseException ex) {
            if (!ex.getStatusCode().is4xxClientError()) {
                throw noDisponible(ex);
            }
            for (String symbol : faltantes) {
                obtenerIndividual(symbol).ifPresent(cotizacion -> {
                    cache.guardar(cotizacion);
                    resultado.put(symbol, cotizacion);
                });
            }
        } catch (RuntimeException ex) {
            throw noDisponible(ex);
        }
        return Map.copyOf(resultado);
    }

    @Override
    public EstadoMercado estadoMercado() {
        try {
            RespuestaEstadoMercado respuesta = solicitud("/api/v1/market/status")
                    .retrieve()
                    .body(RespuestaEstadoMercado.class);
            if (respuesta == null || respuesta.estado() == null) {
                throw new IllegalStateException("M4 no devolvio un estado de mercado valido");
            }
            return new EstadoMercado(respuesta.estado(), respuesta.consultadoEn());
        } catch (RuntimeException ex) {
            throw noDisponible(ex);
        }
    }

    private List<RespuestaCotizacion> obtenerLote(Set<String> symbols) {
        List<RespuestaCotizacion> respuesta = solicitud("/api/v1/quotes/latest?symbols=" + String.join(",", symbols))
                .retrieve()
                .body(LISTA_COTIZACIONES);
        return respuesta == null ? List.of() : respuesta;
    }

    private java.util.Optional<CotizacionMercado> obtenerIndividual(String symbol) {
        try {
            RespuestaCotizacion respuesta = solicitud("/api/v1/quotes/" + symbol)
                    .retrieve()
                    .body(RespuestaCotizacion.class);
            return java.util.Optional.ofNullable(respuesta).map(RespuestaCotizacion::aCotizacion);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().is4xxClientError()) {
                return java.util.Optional.empty();
            }
            throw noDisponible(ex);
        } catch (RuntimeException ex) {
            throw noDisponible(ex);
        }
    }

    private RestClient.RequestHeadersSpec<?> solicitud(String ruta) {
        return cliente.get().uri(ruta).headers(headers -> {
            headers.setBearerAuth(tokenServicio.obtenerToken());
            String correlationId = MDC.get(CorrelationIdFilter.CLAVE_MDC);
            if (correlationId != null && !correlationId.isBlank()) {
                headers.set(CORRELATION_ID, correlationId);
            }
        });
    }

    private static Map<String, CotizacionMercado> convertir(List<RespuestaCotizacion> respuestas) {
        Map<String, CotizacionMercado> cotizaciones = new LinkedHashMap<>();
        for (RespuestaCotizacion respuesta : respuestas) {
            CotizacionMercado cotizacion = respuesta.aCotizacion();
            cotizaciones.put(cotizacion.symbol(), cotizacion);
        }
        return cotizaciones;
    }

    private static DatosMercadoNoDisponibleException noDisponible(Throwable causa) {
        return new DatosMercadoNoDisponibleException("M4 no esta disponible", causa);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RespuestaCotizacion(String symbol, BigDecimal price, Instant timestamp) {
        private CotizacionMercado aCotizacion() {
            return new CotizacionMercado(Simbolos.normalizar(symbol), price, timestamp);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RespuestaEstadoMercado(String estado, Instant consultadoEn) {
    }
}

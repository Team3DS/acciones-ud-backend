package co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.adapter.out.rest;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.Cotizacion;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.DatosMercadoNoDisponibleException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.CotizacionClientPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.Simbolo;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.config.CabecerasServicio;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security.EmisorTokenServicio;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class DatosMercadoRestClient implements CotizacionClientPort {

    private static final ParameterizedTypeReference<List<RespuestaCotizacion>> LISTA_COTIZACIONES =
            new ParameterizedTypeReference<>() { };

    private final RestClient cliente;
    private final EmisorTokenServicio tokenServicio;
    private final CacheCotizaciones cache;

    public DatosMercadoRestClient(@Qualifier("m4RestClient") RestClient cliente,
                                  EmisorTokenServicio tokenServicio, CacheCotizaciones cache) {
        this.cliente = cliente;
        this.tokenServicio = tokenServicio;
        this.cache = cache;
    }

    @Override
    public Map<String, Cotizacion> obtenerCotizaciones(Set<String> symbols) {
        Map<String, Cotizacion> resultado = new LinkedHashMap<>();
        Set<String> faltantes = new LinkedHashSet<>();
        for (String original : symbols) {
            String symbol = Simbolo.desde(original).valor();
            Optional<Cotizacion> enCache = cache.obtener(symbol);
            if (enCache.isPresent()) {
                resultado.put(symbol, enCache.get());
            } else {
                faltantes.add(symbol);
            }
        }
        if (faltantes.isEmpty()) {
            return Map.copyOf(resultado);
        }
        try {
            convertir(obtenerLote(faltantes)).values().forEach(cotizacion -> {
                cache.guardar(cotizacion);
                resultado.put(cotizacion.symbol(), cotizacion);
            });
        } catch (RestClientResponseException ex) {
            if (!ex.getStatusCode().is4xxClientError()) {
                throw new DatosMercadoNoDisponibleException(ex);
            }
            for (String symbol : faltantes) {
                Optional<Cotizacion> cotizacion = obtenerIndividual(symbol);
                if (cotizacion.isPresent()) {
                    cache.guardar(cotizacion.get());
                    resultado.put(symbol, cotizacion.get());
                }
            }
        } catch (RuntimeException ex) {
            throw new DatosMercadoNoDisponibleException(ex);
        }
        return Map.copyOf(resultado);
    }

    private List<RespuestaCotizacion> obtenerLote(Set<String> symbols) {
        List<RespuestaCotizacion> respuesta = CabecerasServicio.aplicar(
                cliente.get().uri(constructor -> constructor
                        .path("/api/v1/quotes/latest")
                        .queryParam("symbols", String.join(",", symbols))
                        .build()),
                tokenServicio.obtenerToken())
                .retrieve()
                .body(LISTA_COTIZACIONES);
        return respuesta == null ? List.of() : respuesta;
    }

    private Optional<Cotizacion> obtenerIndividual(String symbol) {
        try {
            RespuestaCotizacion respuesta = CabecerasServicio.aplicar(
                    cliente.get().uri("/api/v1/quotes/{symbol}", symbol),
                    tokenServicio.obtenerToken())
                    .retrieve()
                    .body(RespuestaCotizacion.class);
            return Optional.ofNullable(respuesta).map(RespuestaCotizacion::aCotizacion);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().is4xxClientError()) {
                return Optional.empty();
            }
            throw new DatosMercadoNoDisponibleException(ex);
        }
    }

    private static Map<String, Cotizacion> convertir(List<RespuestaCotizacion> respuestas) {
        Map<String, Cotizacion> cotizaciones = new LinkedHashMap<>();
        for (RespuestaCotizacion respuesta : respuestas) {
            Cotizacion cotizacion = respuesta.aCotizacion();
            cotizaciones.put(cotizacion.symbol(), cotizacion);
        }
        return cotizaciones;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RespuestaCotizacion(
            String symbol, BigDecimal price, BigDecimal change, BigDecimal changePct, Instant timestamp) {

        private Cotizacion aCotizacion() {
            return new Cotizacion(Simbolo.desde(symbol).valor(), price, change, changePct, timestamp);
        }
    }
}

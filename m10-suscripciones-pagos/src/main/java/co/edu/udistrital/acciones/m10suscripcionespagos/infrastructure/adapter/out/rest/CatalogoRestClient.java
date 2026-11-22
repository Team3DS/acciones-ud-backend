package co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.adapter.out.rest;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.CatalogoNoDisponibleException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.CatalogoClientPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.Simbolo;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.config.CabecerasServicio;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security.EmisorTokenServicio;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class CatalogoRestClient implements CatalogoClientPort {

    private final RestClient cliente;
    private final EmisorTokenServicio tokenServicio;

    public CatalogoRestClient(@Qualifier("m3RestClient") RestClient cliente,
                              EmisorTokenServicio tokenServicio) {
        this.cliente = cliente;
        this.tokenServicio = tokenServicio;
    }

    @Override
    public boolean esValido(Simbolo simbolo) {
        try {
            RespuestaValidacion respuesta = CabecerasServicio.aplicar(
                    cliente.get().uri("/api/v1/instruments/{symbol}/validation", simbolo.valor()),
                    tokenServicio.obtenerToken())
                    .retrieve()
                    .body(RespuestaValidacion.class);
            return respuesta != null && respuesta.valid();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().is4xxClientError()) {
                return false;
            }
            throw new CatalogoNoDisponibleException(ex);
        } catch (RuntimeException ex) {
            throw new CatalogoNoDisponibleException(ex);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RespuestaValidacion(boolean valid) {
    }
}

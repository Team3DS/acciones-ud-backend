package co.edu.udistrital.acciones.m6portafolio.application.port.out;

import co.edu.udistrital.acciones.m6portafolio.application.dto.CotizacionMercado;
import co.edu.udistrital.acciones.m6portafolio.application.dto.EstadoMercado;

import java.util.Map;
import java.util.Set;

public interface DatosMercadoClientPort {

    Map<String, CotizacionMercado> obtenerCotizaciones(Set<String> symbols);

    default EstadoMercado estadoMercado() {
        throw new UnsupportedOperationException("La consulta de estado de mercado no esta disponible");
    }
}

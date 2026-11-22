package co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.Cotizacion;

import java.util.Map;
import java.util.Set;

public interface CotizacionClientPort {

    Map<String, Cotizacion> obtenerCotizaciones(Set<String> symbols);
}

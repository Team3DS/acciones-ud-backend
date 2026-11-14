package co.edu.udistrital.acciones.m4datosmercado.application.port.in;

import co.edu.udistrital.acciones.m4datosmercado.domain.Cotizacion;

import java.util.List;

public interface ObtenerCotizacionUseCase {
    Cotizacion obtenerCotizacion(String symbol);
    List<Cotizacion> obtenerCotizaciones(List<String> symbols);
}
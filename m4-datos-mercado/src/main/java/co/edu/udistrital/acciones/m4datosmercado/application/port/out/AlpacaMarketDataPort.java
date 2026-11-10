package co.edu.udistrital.acciones.m4datosmercado.application.port.out;

import co.edu.udistrital.acciones.m4datosmercado.domain.Cotizacion;
import co.edu.udistrital.acciones.m4datosmercado.domain.HistoricoCotizacion;
import co.edu.udistrital.acciones.m4datosmercado.domain.Intervalo;
import java.time.Instant;
import java.util.List;

public interface AlpacaMarketDataPort {
    Cotizacion obtenerCotizacion(String symbol);
    List<Cotizacion> obtenerCotizaciones(List<String> symbols);
    List<HistoricoCotizacion> obtenerHistorico(String symbol, Instant from, Instant to, Intervalo intervalo);
}

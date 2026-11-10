package co.edu.udistrital.acciones.m4datosmercado.application.port.out;

import co.edu.udistrital.acciones.m4datosmercado.domain.Cotizacion;

public interface WebSocketPort {
    void broadcastCotizacion(Cotizacion cotizacion);
    void suscribir(String sessionId, java.util.List<String> symbols);
    void desuscribir(String sessionId);
}

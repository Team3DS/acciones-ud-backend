package co.edu.udistrital.acciones.m4datosmercado.application.port.out;

import co.edu.udistrital.acciones.m4datosmercado.domain.Cotizacion;

import java.util.List;

public interface WebSocketPort {
    void broadcastCotizacion(Cotizacion cotizacion);
    void suscribir(String sessionId, List<String> symbols);
    void desuscribir(String sessionId);
}
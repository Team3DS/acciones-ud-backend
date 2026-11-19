package co.edu.udistrital.acciones.m4datosmercado.application.port.in;

import java.util.List;

public interface WebSocketCotizacionesUseCase {
    void suscribir(String sessionId, List<String> symbols);
    void desuscribir(String sessionId);
}
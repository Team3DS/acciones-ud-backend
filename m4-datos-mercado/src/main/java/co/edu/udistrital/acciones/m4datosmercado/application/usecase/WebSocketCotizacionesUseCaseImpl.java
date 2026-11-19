package co.edu.udistrital.acciones.m4datosmercado.application.usecase;

import co.edu.udistrital.acciones.m4datosmercado.application.port.in.WebSocketCotizacionesUseCase;
import co.edu.udistrital.acciones.m4datosmercado.application.port.out.WebSocketPort;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WebSocketCotizacionesUseCaseImpl implements WebSocketCotizacionesUseCase {

    private final WebSocketPort webSocketPort;

    public WebSocketCotizacionesUseCaseImpl(WebSocketPort webSocketPort) {
        this.webSocketPort = webSocketPort;
    }

    @Override
    public void suscribir(String sessionId, List<String> symbols) {
        webSocketPort.suscribir(sessionId, symbols);
    }

    @Override
    public void desuscribir(String sessionId) {
        webSocketPort.desuscribir(sessionId);
    }
}
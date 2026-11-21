package co.edu.udistrital.acciones.m4datosmercado.infrastructure.adapter.out.messaging;

import co.edu.udistrital.acciones.m4datosmercado.application.port.out.WebSocketPort;
import co.edu.udistrital.acciones.m4datosmercado.domain.Cotizacion;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;

@Component
public class WebSocketAdapter implements WebSocketPort {

    private final SimpMessagingTemplate messagingTemplate;
    private final Map<String, List<String>> suscripciones = new ConcurrentHashMap<>();

    public WebSocketAdapter(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void broadcastCotizacion(Cotizacion cotizacion) {
        messagingTemplate.convertAndSend("/topic/quotes/" + cotizacion.symbol(), cotizacion);
        messagingTemplate.convertAndSend("/topic/quotes", cotizacion);
    }

    @Override
    public void suscribir(String sessionId, List<String> symbols) {
        suscripciones.put(sessionId, symbols);
    }

    @Override
    public void desuscribir(String sessionId) {
        suscripciones.remove(sessionId);
    }

    public Map<String, List<String>> getSuscripciones() {
        return Map.copyOf(suscripciones);
    }
}
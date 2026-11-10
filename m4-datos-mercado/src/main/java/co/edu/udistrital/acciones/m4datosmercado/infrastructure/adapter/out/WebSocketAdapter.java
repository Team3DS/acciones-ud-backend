package co.edu.udistrital.acciones.m4datosmercado.infrastructure.adapter.out;

import co.edu.udistrital.acciones.m4datosmercado.application.port.out.WebSocketPort;
import co.edu.udistrital.acciones.m4datosmercado.domain.Cotizacion;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketAdapter implements WebSocketPort {
    private final SimpMessagingTemplate template;
    private final ConcurrentHashMap<String, Set<String>> suscripciones = new ConcurrentHashMap<>();

    public WebSocketAdapter(SimpMessagingTemplate template) {
        this.template = template;
    }

    @Override
    public void broadcastCotizacion(Cotizacion cotizacion) {
        template.convertAndSend("/topic/quotes/" + cotizacion.symbol(), cotizacion);
    }

    @Override
    public void suscribir(String sessionId, java.util.List<String> symbols) {
        suscripciones.put(sessionId, Set.copyOf(symbols));
    }

    @Override
    public void desuscribir(String sessionId) {
        suscripciones.remove(sessionId);
    }
}

package co.edu.udistrital.acciones.m4datosmercado.infrastructure.adapter.in.ws;

import co.edu.udistrital.acciones.m4datosmercado.application.port.in.WebSocketCotizacionesUseCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.context.event.EventListener;

import java.util.List;

@Controller
public class CotizacionWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(CotizacionWebSocketHandler.class);

    private final WebSocketCotizacionesUseCase webSocketUseCase;

    public CotizacionWebSocketHandler(WebSocketCotizacionesUseCase webSocketUseCase) {
        this.webSocketUseCase = webSocketUseCase;
    }

    @MessageMapping("/quotes/subscribe/{symbols}")
    @SendTo("/topic/quotes")
    public void suscribir(@DestinationVariable String symbols, StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        List<String> symbolList = List.of(symbols.split(","));
        webSocketUseCase.suscribir(sessionId, symbolList);
        log.debug("Sesión {} suscrita a: {}", sessionId, symbols);
    }

    @MessageMapping("/quotes/unsubscribe")
    public void desuscribir(StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        webSocketUseCase.desuscribir(sessionId);
        log.debug("Sesión {} desuscrita", sessionId);
    }

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        String sessionId = event.getMessage().getHeaders().get("simpSessionId", String.class);
        log.debug("WebSocket conectado: {}", sessionId);
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        webSocketUseCase.desuscribir(sessionId);
        log.debug("WebSocket desconectado: {}", sessionId);
    }
}
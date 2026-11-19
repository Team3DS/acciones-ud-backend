package co.edu.udistrital.acciones.m4datosmercado.application.service;

import co.edu.udistrital.acciones.m4datosmercado.application.port.out.AlpacaMarketDataPort;
import co.edu.udistrital.acciones.m4datosmercado.application.port.out.CatalogoClientPort;
import co.edu.udistrital.acciones.m4datosmercado.application.port.out.WebSocketPort;
import co.edu.udistrital.acciones.m4datosmercado.domain.Cotizacion;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CotizacionSchedulerService {

    private static final List<String> SIMBOLOS_POR_DEFECTO = List.of(
            "AAPL", "TSLA", "MSFT", "GOOGL", "AMZN", "META", "NVDA", "JNJ", "JPM", "XOM"
    );

    private final AlpacaMarketDataPort alpacaPort;
    private final WebSocketPort webSocketPort;
    private final CatalogoClientPort catalogoPort;

    public CotizacionSchedulerService(AlpacaMarketDataPort alpacaPort, WebSocketPort webSocketPort, CatalogoClientPort catalogoPort) {
        this.alpacaPort = alpacaPort;
        this.webSocketPort = webSocketPort;
        this.catalogoPort = catalogoPort;
    }

    @Scheduled(fixedDelay = 15000)
    public void actualizarYTransmitirCotizaciones() {
        List<String> simbolosValidos = SIMBOLOS_POR_DEFECTO.stream()
                .filter(catalogoPort::validarSimbolo)
                .toList();

        if (simbolosValidos.isEmpty()) {
            return;
        }

        List<Cotizacion> cotizaciones = alpacaPort.obtenerCotizaciones(simbolosValidos);
        cotizaciones.forEach(webSocketPort::broadcastCotizacion);
    }
}
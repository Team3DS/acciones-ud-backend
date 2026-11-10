package co.edu.udistrital.acciones.m4datosmercado.infrastructure.config;

import co.edu.udistrital.acciones.m4datosmercado.application.port.out.WebSocketPort;
import co.edu.udistrital.acciones.m4datosmercado.application.usecase.ObtenerCotizacionUseCase;
import co.edu.udistrital.acciones.m4datosmercado.domain.Cotizacion;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@EnableScheduling
public class QuoteScheduler {
    private final ObtenerCotizacionUseCase useCase;
    private final WebSocketPort webSocket;

    public QuoteScheduler(ObtenerCotizacionUseCase useCase, WebSocketPort webSocket) {
        this.useCase = useCase;
        this.webSocket = webSocket;
    }

    @Scheduled(fixedRateString = "${app.market.update-interval:1000}")
    public void refreshAndBroadcast() {
        List<String> symbols = List.of("AAPL", "TSLA", "MSFT");
        for (String s : symbols) {
            try {
                Cotizacion c = useCase.execute(s);
                webSocket.broadcastCotizacion(c);
            } catch (Exception ignored) {}
        }
    }
}

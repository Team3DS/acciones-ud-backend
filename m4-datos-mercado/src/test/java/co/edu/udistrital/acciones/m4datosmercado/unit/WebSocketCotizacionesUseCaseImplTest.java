package co.edu.udistrital.acciones.m4datosmercado.unit;

import co.edu.udistrital.acciones.m4datosmercado.application.port.in.WebSocketCotizacionesUseCase;
import co.edu.udistrital.acciones.m4datosmercado.application.port.out.WebSocketPort;
import co.edu.udistrital.acciones.m4datosmercado.application.usecase.WebSocketCotizacionesUseCaseImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketCotizacionesUseCaseImplTest {

    @Mock
    WebSocketPort webSocketPort;

    WebSocketCotizacionesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new WebSocketCotizacionesUseCaseImpl(webSocketPort);
    }

    @Test
    void suscribir() {
        useCase.suscribir("session-1", List.of("AAPL", "TSLA"));
        verify(webSocketPort).suscribir("session-1", List.of("AAPL", "TSLA"));
    }

    @Test
    void desuscribir() {
        useCase.desuscribir("session-1");
        verify(webSocketPort).desuscribir("session-1");
    }
}
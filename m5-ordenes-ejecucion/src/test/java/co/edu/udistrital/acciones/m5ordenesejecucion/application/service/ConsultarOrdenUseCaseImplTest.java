package co.edu.udistrital.acciones.m5ordenesejecucion.application.service;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.ConsultarOrdenUseCase;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.EjecucionOrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.HistorialEstadoOrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.OrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EjecucionOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EstadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.HistorialEstadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.Orden;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsultarOrdenUseCaseImplTest {

    @Mock
    OrdenRepositoryPort ordenRepository;

    @Mock
    EjecucionOrdenRepositoryPort ejecucionRepository;

    @Mock
    HistorialEstadoOrdenRepositoryPort historialRepository;

    private ConsultarOrdenUseCaseImpl service;

    @BeforeEach
    void setUp() {
        service = new ConsultarOrdenUseCaseImpl(ordenRepository, ejecucionRepository, historialRepository);
    }

    @Test
    void buscarPorIdExistente() {
        Orden orden = new Orden();
        orden.setId(1L);
        orden.setUsuarioId(1L);
        orden.setTicker("AAPL");
        orden.setLadoOrden(co.edu.udistrital.acciones.m5ordenesejecucion.domain.LadoOrden.COMPRA);
        orden.setTipoOrden(co.edu.udistrital.acciones.m5ordenesejecucion.domain.TipoOrden.MARKET);
        orden.setCantidad(10);
        orden.setPrecioLimite(new BigDecimal("150.00"));
        orden.setEstadoOrden(EstadoOrden.PENDIENTE);
        orden.setClientOrderId(UUID.randomUUID());
        orden.setCreadaEn(Instant.now());

        when(ordenRepository.findById(1L)).thenReturn(java.util.Optional.of(orden));

        Orden resultado = service.buscarPorId(1L);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        assertEquals("AAPL", resultado.getTicker());
        verify(ordenRepository).findById(1L);
    }

    @Test
    void buscarPorIdNoExistenteLanzaExcepcion() {
        when(ordenRepository.findById(999L)).thenReturn(java.util.Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.buscarPorId(999L));
    }

    @Test
    void buscarPorUsuario() {
        Orden orden = new Orden();
        orden.setId(1L);
        orden.setUsuarioId(1L);
        orden.setTicker("AAPL");
        orden.setEstadoOrden(EstadoOrden.PENDIENTE);

        when(ordenRepository.findByUsuarioIdAndFilters(1L, EstadoOrden.PENDIENTE, "AAPL"))
                .thenReturn(List.of(orden));

        List<Orden> resultado = service.buscarPorUsuario(1L, EstadoOrden.PENDIENTE, "AAPL");

        assertEquals(1, resultado.size());
        assertEquals("AAPL", resultado.get(0).getTicker());
    }

    @Test
    void buscarFillsPorOrden() {
        EjecucionOrden fill = new EjecucionOrden();
        fill.setId(1L);
        fill.setOrdenId(10L);

        when(ejecucionRepository.findByOrdenId(10L)).thenReturn(List.of(fill));

        List<EjecucionOrden> resultado = service.buscarFillsPorOrden(10L);

        assertEquals(1, resultado.size());
        assertEquals(10L, resultado.get(0).getOrdenId());
    }

    @Test
    void buscarHistorialPorOrden() {
        HistorialEstadoOrden historial = new HistorialEstadoOrden();
        historial.setId(1L);
        historial.setOrdenId(10L);

        when(historialRepository.findByOrdenId(10L)).thenReturn(List.of(historial));

        List<HistorialEstadoOrden> resultado = service.buscarHistorialPorOrden(10L);

        assertEquals(1, resultado.size());
        assertEquals(10L, resultado.get(0).getOrdenId());
    }
}

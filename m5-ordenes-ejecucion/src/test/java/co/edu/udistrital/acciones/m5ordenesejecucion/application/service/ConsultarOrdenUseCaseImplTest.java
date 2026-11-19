package co.edu.udistrital.acciones.m5ordenesejecucion.application.service;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.CrearOrdenUseCase.OrdenResultado;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.EjecucionRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.OrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EstadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.LadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.Orden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.TipoOrden;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsultarOrdenUseCaseImplTest {

    @Mock
    OrdenRepositoryPort ordenRepository;

    @Mock
    EjecucionRepositoryPort ejecucionRepository;

    private ConsultarOrdenUseCaseImpl service;

    @BeforeEach
    void setUp() {
        service = new ConsultarOrdenUseCaseImpl(ordenRepository, ejecucionRepository);
    }

    @Test
    void buscarPorIdExistente() {
        Orden orden = new Orden();
        orden.setId(1L);
        orden.setUsuarioId(1L);
        orden.setSimbolo("AAPL");
        orden.setLado(LadoOrden.COMPRA);
        orden.setTipo(TipoOrden.MARKET);
        orden.setCantidad(BigDecimal.TEN);
        orden.setPrecioLimite(new BigDecimal("150.00"));
        orden.setComision(new BigDecimal("3.00"));
        orden.setValorTotal(new BigDecimal("1503.00"));
        orden.setEstado(EstadoOrden.PENDIENTE);
        orden.setClientOrderId("idem-1");

        when(ordenRepository.findById(1L)).thenReturn(java.util.Optional.of(orden));

        Orden resultado = service.buscarPorId(1L);
        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        assertEquals("AAPL", resultado.getSimbolo());
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
        orden.setSimbolo("AAPL");
        orden.setLado(LadoOrden.COMPRA);
        orden.setTipo(TipoOrden.MARKET);
        orden.setCantidad(BigDecimal.TEN);
        orden.setPrecioLimite(new BigDecimal("150.00"));
        orden.setComision(new BigDecimal("3.00"));
        orden.setValorTotal(new BigDecimal("1503.00"));
        orden.setEstado(EstadoOrden.PENDIENTE);
        orden.setClientOrderId("idem-1");

        when(ordenRepository.findByUsuarioId(1L)).thenReturn(List.of(orden));

        List<Orden> resultado = service.buscarPorUsuario(1L);
        assertEquals(1, resultado.size());
        assertEquals("AAPL", resultado.get(0).getSimbolo());
    }

    @Test
    void resumenOrden() {
        Orden orden = new Orden();
        orden.setId(1L);
        orden.setSimbolo("AAPL");
        orden.setLado(LadoOrden.COMPRA);
        orden.setTipo(TipoOrden.MARKET);
        orden.setCantidad(BigDecimal.TEN);
        orden.setPrecioLimite(new BigDecimal("150.00"));
        orden.setComision(new BigDecimal("3.00"));
        orden.setValorTotal(new BigDecimal("1503.00"));
        orden.setEstado(EstadoOrden.PENDIENTE);
        orden.setClientOrderId("idem-1");

        when(ordenRepository.findById(1L)).thenReturn(java.util.Optional.of(orden));

        OrdenResultado resultado = service.resumen(1L);
        assertEquals(1L, resultado.id());
        assertEquals("AAPL", resultado.simbolo());
        assertEquals(new BigDecimal("3.00"), resultado.comision());
    }
}

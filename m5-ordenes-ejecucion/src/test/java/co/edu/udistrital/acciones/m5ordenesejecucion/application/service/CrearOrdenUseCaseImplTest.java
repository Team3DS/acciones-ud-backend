package co.edu.udistrital.acciones.m5ordenesejecucion.application.service;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.CrearOrdenUseCase.CrearOrdenCommand;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.CrearOrdenUseCase.OrdenResultado;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.ComisionRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.OrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.PublicadorOrdenesPort;
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
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrearOrdenUseCaseImplTest {

    @Mock
    OrdenRepositoryPort ordenRepository;

    @Mock
    ComisionRepositoryPort comisionRepository;

    @Mock
    PublicadorOrdenesPort publicador;

    private CrearOrdenUseCaseImpl service;

    @BeforeEach
    void setUp() {
        service = new CrearOrdenUseCaseImpl(ordenRepository, comisionRepository, publicador);
    }

    @Test
    void crearOrdenExitosa() {
        when(ordenRepository.findByClientOrderId("idem-1")).thenReturn(java.util.Optional.empty());
        when(ordenRepository.save(any())).thenAnswer(inv -> {
            Orden o = inv.getArgument(0);
            o.setId(1L);
            return o;
        });

        CrearOrdenCommand cmd = new CrearOrdenCommand(
                1L, "AAPL", "COMPRA", "MARKET", 10, new BigDecimal("150.00"), "idem-1"
        );

        Orden resultado = service.ejecutar(cmd);

        assertNotNull(resultado);
        assertEquals(EstadoOrden.PENDIENTE, resultado.getEstado());
        assertEquals("AAPL", resultado.getSimbolo());
        assertEquals(LadoOrden.COMPRA, resultado.getLado());
        verify(publicador).publicarOrdenCreada(anyLong(), anyLong(), anyString(), anyString(), anyInt(), anyString());
    }

    @Test
    void crearOrdenDuplicadaLanzaExcepcion() {
        when(ordenRepository.findByClientOrderId("idem-1")).thenReturn(java.util.Optional.of(new Orden()));

        CrearOrdenCommand cmd = new CrearOrdenCommand(
                1L, "AAPL", "COMPRA", "MARKET", 10, new BigDecimal("150.00"), "idem-1"
        );

        assertThrows(IllegalArgumentException.class, () -> service.ejecutar(cmd));
        verifyNoInteractions(publicador);
    }

    @Test
    void convertirOrdenResultado() {
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

        OrdenResultado resultado = service.convertir(orden);

        assertEquals(1L, resultado.id());
        assertEquals("AAPL", resultado.simbolo());
        assertEquals("COMPRA", resultado.lado());
        assertEquals(new BigDecimal("3.00"), resultado.comision());
    }
}

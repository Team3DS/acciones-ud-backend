package co.edu.udistrital.acciones.m5ordenesejecucion.application.service;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.ConsultarEjecucionesUseCase;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.ConfirmacionEjecucionRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.EjecucionOrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.OrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.ConfirmacionEjecucion;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EjecucionOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.LadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.Orden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.ServicioParticipante;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsultarEjecucionesUseCaseImplTest {

    @Mock
    EjecucionOrdenRepositoryPort ejecucionRepository;

    @Mock
    ConfirmacionEjecucionRepositoryPort confirmacionRepository;

    @Mock
    OrdenRepositoryPort ordenRepository;

    private ConsultarEjecucionesUseCaseImpl service;

    @BeforeEach
    void setUp() {
        service = new ConsultarEjecucionesUseCaseImpl(ejecucionRepository, confirmacionRepository, ordenRepository);
    }

    @Test
    void buscarPorIdExistente() {
        EjecucionOrden ejecucion = new EjecucionOrden();
        ejecucion.setId(1L);
        ejecucion.setOrdenId(10L);

        when(ejecucionRepository.findById(1L)).thenReturn(Optional.of(ejecucion));

        EjecucionOrden resultado = service.buscarPorId(1L);

        assertEquals(1L, resultado.getId());
        assertEquals(10L, resultado.getOrdenId());
    }

    @Test
    void buscarPorIdNoExistenteLanzaExcepcion() {
        when(ejecucionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.buscarPorId(999L));
    }

    @Test
    void resumenSinEjecuciones() {
        Instant desde = Instant.now().minusSeconds(3600);
        Instant hasta = Instant.now();

        when(ejecucionRepository.findByUsuarioIdAndPeriodo(1L, desde, hasta)).thenReturn(List.of());

        ConsultarEjecucionesUseCase.ResumenEjecucionesResponse resumen = service.resumen(1L, desde, hasta);

        assertEquals(1L, resumen.usuarioId());
        assertEquals(0, resumen.gananciaBrutaRealizada().compareTo(BigDecimal.ZERO));
        assertEquals(0, resumen.comisionesCompra().compareTo(BigDecimal.ZERO));
        assertEquals(0, resumen.comisionesVenta().compareTo(BigDecimal.ZERO));
        assertEquals(0, resumen.ventasPendientesConfirmacion());
    }

    @Test
    void resumenCompraSinComision() {
        Instant desde = Instant.now().minusSeconds(3600);
        Instant hasta = Instant.now();

        EjecucionOrden ejecucion = new EjecucionOrden();
        ejecucion.setId(1L);
        ejecucion.setOrdenId(10L);
        ejecucion.setComision(null);

        Orden orden = new Orden();
        orden.setId(10L);
        orden.setLadoOrden(LadoOrden.COMPRA);

        when(ejecucionRepository.findByUsuarioIdAndPeriodo(1L, desde, hasta)).thenReturn(List.of(ejecucion));
        when(ordenRepository.findById(10L)).thenReturn(Optional.of(orden));

        ConsultarEjecucionesUseCase.ResumenEjecucionesResponse resumen = service.resumen(1L, desde, hasta);

        assertEquals(0, resumen.comisionesCompra().compareTo(BigDecimal.ZERO));
        assertEquals(0, resumen.gananciaBrutaRealizada().compareTo(BigDecimal.ZERO));
    }

    @Test
    void resumenCompraConComision() {
        Instant desde = Instant.now().minusSeconds(3600);
        Instant hasta = Instant.now();

        EjecucionOrden ejecucion = new EjecucionOrden();
        ejecucion.setId(1L);
        ejecucion.setOrdenId(10L);
        ejecucion.setComision(new BigDecimal("2.50"));

        Orden orden = new Orden();
        orden.setId(10L);
        orden.setLadoOrden(LadoOrden.COMPRA);

        when(ejecucionRepository.findByUsuarioIdAndPeriodo(1L, desde, hasta)).thenReturn(List.of(ejecucion));
        when(ordenRepository.findById(10L)).thenReturn(Optional.of(orden));

        ConsultarEjecucionesUseCase.ResumenEjecucionesResponse resumen = service.resumen(1L, desde, hasta);

        assertEquals(0, new BigDecimal("2.50").compareTo(resumen.comisionesCompra()));
        assertEquals(0, resumen.gananciaBrutaRealizada().compareTo(BigDecimal.ZERO));
    }

    @Test
    void resumenVentaConConfirmacionPortafolio() {
        Instant desde = Instant.now().minusSeconds(3600);
        Instant hasta = Instant.now();

        EjecucionOrden ejecucion = new EjecucionOrden();
        ejecucion.setId(1L);
        ejecucion.setOrdenId(10L);
        ejecucion.setCantidad(10);
        ejecucion.setPrecioEjecucion(new BigDecimal("160.00"));
        ejecucion.setComision(new BigDecimal("1.00"));

        Orden orden = new Orden();
        orden.setId(10L);
        orden.setLadoOrden(LadoOrden.VENTA);

        ConfirmacionEjecucion confirmacion = new ConfirmacionEjecucion();
        confirmacion.setPrecioPromedioCosto(new BigDecimal("150.00"));

        when(ejecucionRepository.findByUsuarioIdAndPeriodo(1L, desde, hasta)).thenReturn(List.of(ejecucion));
        when(ordenRepository.findById(10L)).thenReturn(Optional.of(orden));
        when(confirmacionRepository.findByEjecucionIdAndServicio(1L, ServicioParticipante.PORTAFOLIO))
                .thenReturn(Optional.of(confirmacion));

        ConsultarEjecucionesUseCase.ResumenEjecucionesResponse resumen = service.resumen(1L, desde, hasta);

        assertEquals(new BigDecimal("1.00"), resumen.comisionesVenta());
        assertEquals(new BigDecimal("100.00"), resumen.gananciaBrutaRealizada());
        assertEquals(0, resumen.ventasPendientesConfirmacion());
    }

    @Test
    void resumenVentaSinConfirmacionPortafolio() {
        Instant desde = Instant.now().minusSeconds(3600);
        Instant hasta = Instant.now();

        EjecucionOrden ejecucion = new EjecucionOrden();
        ejecucion.setId(1L);
        ejecucion.setOrdenId(10L);
        ejecucion.setCantidad(10);
        ejecucion.setPrecioEjecucion(new BigDecimal("160.00"));
        ejecucion.setComision(new BigDecimal("1.00"));

        Orden orden = new Orden();
        orden.setId(10L);
        orden.setLadoOrden(LadoOrden.VENTA);

        when(ejecucionRepository.findByUsuarioIdAndPeriodo(1L, desde, hasta)).thenReturn(List.of(ejecucion));
        when(ordenRepository.findById(10L)).thenReturn(Optional.of(orden));
        when(confirmacionRepository.findByEjecucionIdAndServicio(1L, ServicioParticipante.PORTAFOLIO))
                .thenReturn(Optional.empty());

        ConsultarEjecucionesUseCase.ResumenEjecucionesResponse resumen = service.resumen(1L, desde, hasta);

        assertEquals(0, new BigDecimal("1.00").compareTo(resumen.comisionesVenta()));
        assertEquals(0, resumen.gananciaBrutaRealizada().compareTo(BigDecimal.ZERO));
        assertEquals(1, resumen.ventasPendientesConfirmacion());
    }

    @Test
    void resumenVentaConfirmacionConPrecioCostoCero() {
        Instant desde = Instant.now().minusSeconds(3600);
        Instant hasta = Instant.now();

        EjecucionOrden ejecucion = new EjecucionOrden();
        ejecucion.setId(1L);
        ejecucion.setOrdenId(10L);
        ejecucion.setCantidad(10);
        ejecucion.setPrecioEjecucion(new BigDecimal("160.00"));
        ejecucion.setComision(new BigDecimal("1.00"));

        Orden orden = new Orden();
        orden.setId(10L);
        orden.setLadoOrden(LadoOrden.VENTA);

        ConfirmacionEjecucion confirmacion = new ConfirmacionEjecucion();
        confirmacion.setPrecioPromedioCosto(BigDecimal.ZERO);

        when(ejecucionRepository.findByUsuarioIdAndPeriodo(1L, desde, hasta)).thenReturn(List.of(ejecucion));
        when(ordenRepository.findById(10L)).thenReturn(Optional.of(orden));
        when(confirmacionRepository.findByEjecucionIdAndServicio(1L, ServicioParticipante.PORTAFOLIO))
                .thenReturn(Optional.of(confirmacion));

        ConsultarEjecucionesUseCase.ResumenEjecucionesResponse resumen = service.resumen(1L, desde, hasta);

        assertEquals(1, resumen.ventasPendientesConfirmacion());
    }

    @Test
    void resumenOrdenNoEncontradaSeIgnora() {
        Instant desde = Instant.now().minusSeconds(3600);
        Instant hasta = Instant.now();

        EjecucionOrden ejecucion = new EjecucionOrden();
        ejecucion.setId(1L);
        ejecucion.setOrdenId(999L);

        when(ejecucionRepository.findByUsuarioIdAndPeriodo(1L, desde, hasta)).thenReturn(List.of(ejecucion));
        when(ordenRepository.findById(999L)).thenReturn(Optional.empty());

        ConsultarEjecucionesUseCase.ResumenEjecucionesResponse resumen = service.resumen(1L, desde, hasta);

        assertEquals(0, resumen.gananciaBrutaRealizada().compareTo(BigDecimal.ZERO));
        assertEquals(0, resumen.comisionesCompra().compareTo(BigDecimal.ZERO));
        assertEquals(0, resumen.comisionesVenta().compareTo(BigDecimal.ZERO));
    }
}

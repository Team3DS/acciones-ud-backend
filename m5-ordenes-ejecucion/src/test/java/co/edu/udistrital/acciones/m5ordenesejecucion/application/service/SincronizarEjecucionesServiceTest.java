package co.edu.udistrital.acciones.m5ordenesejecucion.application.service;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.ProcesarEjecucionUseCase;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.ProcesarEjecucionUseCase.ProcesarEjecucionCommand;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.AlpacaOrderPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.AlpacaOrderPort.AlpacaOrderResponse;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.DatosMercadoClientPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.EjecucionOrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.OrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EjecucionOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.LadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.Orden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.TipoOrden;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SincronizarEjecucionesServiceTest {

    private OrdenRepositoryPort ordenRepository;
    private EjecucionOrdenRepositoryPort ejecucionRepository;
    private AlpacaOrderPort alpaca;
    private DatosMercadoClientPort datosMercado;
    private ProcesarEjecucionUseCase procesarEjecucion;
    private SincronizarEjecucionesService service;

    @BeforeEach
    void setUp() {
        ordenRepository = mock(OrdenRepositoryPort.class);
        ejecucionRepository = mock(EjecucionOrdenRepositoryPort.class);
        alpaca = mock(AlpacaOrderPort.class);
        datosMercado = mock(DatosMercadoClientPort.class);
        procesarEjecucion = mock(ProcesarEjecucionUseCase.class);
        service = new SincronizarEjecucionesService(ordenRepository, ejecucionRepository, alpaca, datosMercado, procesarEjecucion);
    }

    private static Orden orden(Long id, String brokerOrderId, TipoOrden tipo, LadoOrden lado, int cantidad) {
        Orden orden = new Orden();
        orden.setId(id);
        orden.setTicker("AAPL");
        orden.setTipoOrden(tipo);
        orden.setLadoOrden(lado);
        orden.setCantidad(cantidad);
        orden.setBrokerOrderId(brokerOrderId);
        return orden;
    }

    private static EjecucionOrden ejecucion(int cantidad, String precio) {
        return new EjecucionOrden(1L, 10L, "exec-prev", cantidad, new BigDecimal(precio), BigDecimal.ONE,
                Instant.now(), Instant.now());
    }

    private ProcesarEjecucionCommand comandoEnviado() {
        ArgumentCaptor<ProcesarEjecucionCommand> captor = ArgumentCaptor.forClass(ProcesarEjecucionCommand.class);
        verify(procesarEjecucion).ejecutar(captor.capture());
        return captor.getValue();
    }

    @Test
    void simuladorEjecutaLaOrdenDeMercadoConElPrecioDeM4() {
        Orden orden = orden(10L, AlpacaOrderPort.PREFIJO_SIMULADO + "abc", TipoOrden.MARKET, LadoOrden.COMPRA, 5);
        when(ordenRepository.findPendientesDeEjecucion()).thenReturn(List.of(orden));
        when(ejecucionRepository.findByOrdenId(10L)).thenReturn(List.of());
        when(datosMercado.obtenerPrecioEstimado("AAPL")).thenReturn(new BigDecimal("228.40"));

        assertEquals(1, service.sincronizar());

        ProcesarEjecucionCommand comando = comandoEnviado();
        assertEquals(10L, comando.ordenId());
        assertEquals(5, comando.cantidad());
        assertEquals(new BigDecimal("228.40"), comando.precioEjecucion());
        assertEquals(AlpacaOrderPort.PREFIJO_SIMULADO + "abc-sim", comando.brokerExecutionId());
        assertNull(comando.comision(), "La comisión la calcula ProcesarEjecucion con la configuración vigente");
        verifyNoInteractions(alpaca);
    }

    @Test
    void simuladorSoloEjecutaLoPendiente() {
        Orden orden = orden(10L, AlpacaOrderPort.PREFIJO_SIMULADO + "abc", TipoOrden.MARKET, LadoOrden.VENTA, 10);
        when(ordenRepository.findPendientesDeEjecucion()).thenReturn(List.of(orden));
        when(ejecucionRepository.findByOrdenId(10L)).thenReturn(List.of(ejecucion(4, "100.00")));
        when(datosMercado.obtenerPrecioEstimado("AAPL")).thenReturn(new BigDecimal("101.00"));

        service.sincronizar();

        assertEquals(6, comandoEnviado().cantidad());
    }

    @Test
    void simuladorRespetaElPrecioLimite() {
        Orden compra = orden(10L, AlpacaOrderPort.PREFIJO_SIMULADO + "a", TipoOrden.LIMIT, LadoOrden.COMPRA, 1);
        compra.setPrecioLimite(new BigDecimal("200.00"));
        when(ordenRepository.findPendientesDeEjecucion()).thenReturn(List.of(compra));
        when(ejecucionRepository.findByOrdenId(10L)).thenReturn(List.of());
        when(datosMercado.obtenerPrecioEstimado("AAPL")).thenReturn(new BigDecimal("228.40"));

        assertEquals(0, service.sincronizar());
        verifyNoInteractions(procesarEjecucion);
    }

    @Test
    void condicionesDeActivacionPorTipoYLado() {
        BigDecimal precio = new BigDecimal("100");
        Orden limiteCompra = orden(1L, "x", TipoOrden.LIMIT, LadoOrden.COMPRA, 1);
        limiteCompra.setPrecioLimite(new BigDecimal("100"));
        Orden limiteVenta = orden(2L, "x", TipoOrden.LIMIT, LadoOrden.VENTA, 1);
        limiteVenta.setPrecioLimite(new BigDecimal("101"));
        Orden stopVenta = orden(3L, "x", TipoOrden.STOP_LOSS, LadoOrden.VENTA, 1);
        stopVenta.setPrecioActivacion(new BigDecimal("100.5"));
        Orden takeProfitVenta = orden(4L, "x", TipoOrden.TAKE_PROFIT, LadoOrden.VENTA, 1);
        takeProfitVenta.setPrecioActivacion(new BigDecimal("110"));
        Orden sinPrecio = orden(5L, "x", TipoOrden.LIMIT, LadoOrden.COMPRA, 1);

        assertTrue(SincronizarEjecucionesService.seActiva(orden(0L, "x", TipoOrden.MARKET, LadoOrden.COMPRA, 1), precio));
        assertTrue(SincronizarEjecucionesService.seActiva(limiteCompra, precio));
        assertFalse(SincronizarEjecucionesService.seActiva(limiteVenta, precio));
        assertTrue(SincronizarEjecucionesService.seActiva(stopVenta, precio));
        assertFalse(SincronizarEjecucionesService.seActiva(takeProfitVenta, precio));
        assertFalse(SincronizarEjecucionesService.seActiva(sinPrecio, precio));
    }

    @Test
    void conAlpacaRegistraSoloLoNuevoConPrecioExacto() {
        Orden orden = orden(10L, "alpaca-123", TipoOrden.MARKET, LadoOrden.COMPRA, 10);
        when(ordenRepository.findPendientesDeEjecucion()).thenReturn(List.of(orden));
        when(ejecucionRepository.findByOrdenId(10L)).thenReturn(List.of(ejecucion(4, "100.00")));
        // Alpaca: 10 ejecutadas a 101.20 de promedio -> las 6 nuevas costaron (1012 - 400) / 6 = 102.0000
        when(alpaca.consultarOrden("alpaca-123")).thenReturn(
                new AlpacaOrderResponse("alpaca-123", null, "filled", "AAPL", 10, new BigDecimal("101.20"), 10));

        assertEquals(1, service.sincronizar());

        ProcesarEjecucionCommand comando = comandoEnviado();
        assertEquals(6, comando.cantidad());
        assertEquals(0, new BigDecimal("102.0000").compareTo(comando.precioEjecucion()));
        assertEquals("alpaca-123-10", comando.brokerExecutionId());
        verifyNoInteractions(datosMercado);
    }

    @Test
    void conAlpacaSinEjecucionesNuevasNoRegistraNada() {
        Orden orden = orden(10L, "alpaca-123", TipoOrden.LIMIT, LadoOrden.COMPRA, 10);
        when(ordenRepository.findPendientesDeEjecucion()).thenReturn(List.of(orden));
        when(ejecucionRepository.findByOrdenId(10L)).thenReturn(List.of(ejecucion(4, "100.00")));
        when(alpaca.consultarOrden("alpaca-123")).thenReturn(
                new AlpacaOrderResponse("alpaca-123", null, "partially_filled", "AAPL", 10, new BigDecimal("100.00"), 4));

        assertEquals(0, service.sincronizar());
        verifyNoInteractions(procesarEjecucion);
    }

    @Test
    void unaOrdenConErrorNoDetieneLasDemas() {
        Orden mala = orden(10L, "alpaca-1", TipoOrden.MARKET, LadoOrden.COMPRA, 1);
        Orden buena = orden(11L, AlpacaOrderPort.PREFIJO_SIMULADO + "b", TipoOrden.MARKET, LadoOrden.COMPRA, 2);
        when(ordenRepository.findPendientesDeEjecucion()).thenReturn(List.of(mala, buena));
        when(ejecucionRepository.findByOrdenId(any())).thenReturn(List.of());
        when(alpaca.consultarOrden(anyString())).thenThrow(new RuntimeException("Alpaca caído"));
        when(datosMercado.obtenerPrecioEstimado("AAPL")).thenReturn(new BigDecimal("50.00"));

        assertEquals(1, service.sincronizar());
        assertEquals(11L, comandoEnviado().ordenId());
    }

    @Test
    void ignoraOrdenesCompletasOSinIdDelBroker() {
        Orden completa = orden(10L, AlpacaOrderPort.PREFIJO_SIMULADO + "a", TipoOrden.MARKET, LadoOrden.COMPRA, 3);
        Orden sinBroker = orden(11L, null, TipoOrden.MARKET, LadoOrden.COMPRA, 3);
        when(ordenRepository.findPendientesDeEjecucion()).thenReturn(List.of(completa, sinBroker));
        when(ejecucionRepository.findByOrdenId(10L)).thenReturn(List.of(ejecucion(3, "10.00")));

        assertEquals(0, service.sincronizar());
        verifyNoInteractions(procesarEjecucion, datosMercado, alpaca);
    }

    @Test
    void precioDeLaParteNuevaUsaElPromedioSiElCalculoNoEsPositivo() {
        BigDecimal precio = SincronizarEjecucionesService.precioDeLaParteNueva(
                new BigDecimal("10.00"), 2, List.of(ejecucion(1, "50.00")), 1);
        assertEquals(new BigDecimal("10.00"), precio);
    }

    @Test
    void simuladorNoEjecutaSinPrecioDeM4() {
        Orden orden = orden(10L, AlpacaOrderPort.PREFIJO_SIMULADO + "a", TipoOrden.MARKET, LadoOrden.COMPRA, 1);
        when(ordenRepository.findPendientesDeEjecucion()).thenReturn(List.of(orden));
        when(ejecucionRepository.findByOrdenId(10L)).thenReturn(List.of());
        when(datosMercado.obtenerPrecioEstimado("AAPL")).thenReturn(null, BigDecimal.ZERO);

        assertEquals(0, service.sincronizar());
        assertEquals(0, service.sincronizar());
        verifyNoInteractions(procesarEjecucion);
    }

    @Test
    void conAlpacaSinDatosDeLlenadoNoRegistraNada() {
        Orden orden = orden(10L, "alpaca-123", TipoOrden.MARKET, LadoOrden.COMPRA, 5);
        when(ordenRepository.findPendientesDeEjecucion()).thenReturn(List.of(orden));
        when(ejecucionRepository.findByOrdenId(10L)).thenReturn(List.of());
        when(alpaca.consultarOrden("alpaca-123")).thenReturn(
                null,
                new AlpacaOrderResponse("alpaca-123", null, "new", "AAPL", 5, null, 0),
                new AlpacaOrderResponse("alpaca-123", null, "accepted", "AAPL", 5, BigDecimal.TEN, null));

        assertEquals(0, service.sincronizar());
        assertEquals(0, service.sincronizar());
        assertEquals(0, service.sincronizar());
        verifyNoInteractions(procesarEjecucion);
    }

    @Test
    void conAlpacaNoRegistraMasQueLaCantidadDeLaOrden() {
        Orden orden = orden(10L, "alpaca-123", TipoOrden.MARKET, LadoOrden.VENTA, 5);
        when(ordenRepository.findPendientesDeEjecucion()).thenReturn(List.of(orden));
        when(ejecucionRepository.findByOrdenId(10L)).thenReturn(List.of());
        when(alpaca.consultarOrden("alpaca-123")).thenReturn(
                new AlpacaOrderResponse("alpaca-123", null, "filled", "AAPL", 5, new BigDecimal("20.00"), 8));

        assertEquals(1, service.sincronizar());
        ProcesarEjecucionCommand comando = comandoEnviado();
        assertEquals(5, comando.cantidad());
        assertEquals("alpaca-123-5", comando.brokerExecutionId());
    }

    @Test
    void ignoraOrdenesSinCantidadOConIdDelBrokerVacio() {
        Orden sinCantidad = orden(10L, "alpaca-1", TipoOrden.MARKET, LadoOrden.COMPRA, 1);
        sinCantidad.setCantidad(null);
        Orden idVacio = orden(11L, " ", TipoOrden.MARKET, LadoOrden.COMPRA, 1);
        when(ordenRepository.findPendientesDeEjecucion()).thenReturn(List.of(sinCantidad, idVacio));

        assertEquals(0, service.sincronizar());
        verifyNoInteractions(ejecucionRepository, procesarEjecucion, datosMercado, alpaca);
    }

    @Test
    void condicionesDeActivacionEnCompraYSinPrecioDeActivacion() {
        BigDecimal precio = new BigDecimal("100");
        Orden stopCompra = orden(1L, "x", TipoOrden.STOP_LOSS, LadoOrden.COMPRA, 1);
        stopCompra.setPrecioActivacion(new BigDecimal("99"));
        Orden takeProfitCompra = orden(2L, "x", TipoOrden.TAKE_PROFIT, LadoOrden.COMPRA, 1);
        takeProfitCompra.setPrecioActivacion(new BigDecimal("101"));
        Orden limiteVentaActiva = orden(3L, "x", TipoOrden.LIMIT, LadoOrden.VENTA, 1);
        limiteVentaActiva.setPrecioLimite(new BigDecimal("100"));

        assertTrue(SincronizarEjecucionesService.seActiva(stopCompra, precio));
        assertTrue(SincronizarEjecucionesService.seActiva(takeProfitCompra, precio));
        assertTrue(SincronizarEjecucionesService.seActiva(limiteVentaActiva, precio));
        assertFalse(SincronizarEjecucionesService.seActiva(orden(4L, "x", TipoOrden.STOP_LOSS, LadoOrden.VENTA, 1), precio));
        assertFalse(SincronizarEjecucionesService.seActiva(orden(5L, "x", TipoOrden.TAKE_PROFIT, LadoOrden.COMPRA, 1), precio));
    }
}

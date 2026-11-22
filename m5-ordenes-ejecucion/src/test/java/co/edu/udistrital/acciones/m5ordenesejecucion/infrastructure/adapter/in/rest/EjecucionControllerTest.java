package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.in.rest;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.ConsultarEjecucionesUseCase;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.ConsultarOrdenUseCase;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EjecucionOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.Orden;
import co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EjecucionControllerTest {

    @Mock
    ConsultarEjecucionesUseCase consultarEjecuciones;

    @Mock
    ConsultarOrdenUseCase consultarOrden;

    private EjecucionController controller;
    private final AuthenticatedUser usuario = new AuthenticatedUser(7L, Set.of("INVERSIONISTA"));

    @BeforeEach
    void setUp() {
        controller = new EjecucionController(consultarEjecuciones, consultarOrden);
    }

    private static ConsultarEjecucionesUseCase.ResumenEjecucionesResponse resumen(Instant desde, Instant hasta) {
        return new ConsultarEjecucionesUseCase.ResumenEjecucionesResponse(
                7L, desde, hasta, new BigDecimal("150.00"), new BigDecimal("4.00"), new BigDecimal("2.00"), 0);
    }

    @Test
    void resumenDelPeriodoDelUsuarioAutenticado() {
        Instant desde = Instant.parse("2026-11-01T00:00:00Z");
        Instant hasta = Instant.parse("2026-11-22T23:59:59Z");
        when(consultarEjecuciones.resumen(7L, desde, hasta)).thenReturn(resumen(desde, hasta));

        ConsultarEjecucionesUseCase.ResumenEjecucionesResponse respuesta =
                controller.resumen("2026-11-01T00:00:00Z", "2026-11-22T23:59:59Z", usuario);

        assertEquals(new BigDecimal("150.00"), respuesta.gananciaBrutaRealizada());
        assertEquals(new BigDecimal("4.00"), respuesta.comisionesCompra());
    }

    @Test
    void resumenSinFechasCubreTodoElHistorial() {
        when(consultarEjecuciones.resumen(7L, null, null)).thenReturn(resumen(null, null));

        assertNotNull(controller.resumen(null, " ", usuario));
        verify(consultarEjecuciones).resumen(7L, null, null);
    }

    @Test
    void rechazaFechasInvalidasOInvertidas() {
        assertThrows(IllegalArgumentException.class, () -> controller.resumen("ayer", null, usuario));
        assertThrows(IllegalArgumentException.class,
                () -> controller.resumen("2026-11-22T00:00:00Z", "2026-11-01T00:00:00Z", usuario));
        verifyNoInteractions(consultarEjecuciones);
    }

    @Test
    void detalleDeUnaEjecucionPropia() {
        EjecucionOrden ejecucion = new EjecucionOrden();
        ejecucion.setId(5L);
        ejecucion.setOrdenId(10L);
        Orden orden = new Orden();
        orden.setId(10L);
        orden.setUsuarioId(7L);
        when(consultarEjecuciones.buscarPorId(5L)).thenReturn(ejecucion);
        when(consultarOrden.buscarPorId(10L)).thenReturn(orden);

        ResponseEntity<EjecucionOrden> respuesta = controller.detalle(5L, usuario);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertSame(ejecucion, respuesta.getBody());
    }

    @Test
    void detalleDeOtroUsuarioEsProhibidoSalvoAdministrador() {
        EjecucionOrden ejecucion = new EjecucionOrden();
        ejecucion.setId(6L);
        ejecucion.setOrdenId(11L);
        Orden orden = new Orden();
        orden.setId(11L);
        orden.setUsuarioId(99L);
        when(consultarEjecuciones.buscarPorId(6L)).thenReturn(ejecucion);
        when(consultarOrden.buscarPorId(11L)).thenReturn(orden);

        assertEquals(HttpStatus.FORBIDDEN, controller.detalle(6L, usuario).getStatusCode());
        AuthenticatedUser admin = new AuthenticatedUser(1L, Set.of(AuthenticatedUser.ROL_ADMINISTRADOR));
        assertEquals(HttpStatus.OK, controller.detalle(6L, admin).getStatusCode());
    }
}

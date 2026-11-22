package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.in.rest;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.CancelarOrdenUseCase;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.ConfirmarEncolamientoUseCase;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.ConsultarOrdenUseCase;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.CrearOrdenUseCase;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EjecucionOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EstadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.LadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.Orden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.TipoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.VigenciaOrden;
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
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrdenControllerTest {

    @Mock
    CrearOrdenUseCase crearOrdenUseCase;

    @Mock
    ConsultarOrdenUseCase consultarOrdenUseCase;

    @Mock
    CancelarOrdenUseCase cancelarOrdenUseCase;

    @Mock
    ConfirmarEncolamientoUseCase confirmarEncolamientoUseCase;

    private OrdenController controller;

    @BeforeEach
    void setUp() {
        controller = new OrdenController(
                crearOrdenUseCase,
                consultarOrdenUseCase,
                cancelarOrdenUseCase,
                confirmarEncolamientoUseCase
        );
    }

    @Test
    void crearOrdenExito() {
        UUID clientOrderId = UUID.randomUUID();
        Orden orden = crearOrden(clientOrderId);

        when(crearOrdenUseCase.ejecutar(any(CrearOrdenUseCase.CrearOrdenCommand.class))).thenReturn(orden);

        OrdenController.CrearOrdenRequest request = new OrdenController.CrearOrdenRequest(
                "AAPL", null, 1L, "COMPRA", "MARKET", "DIA", 10,
                null, null, null, null, null
        );
        AuthenticatedUser usuario = new AuthenticatedUser(1L, Set.of("USER"));

        ResponseEntity<OrdenController.OrdenDetalleResponse> response = controller.crearOrden(request, null, usuario);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().id());
        assertEquals("AAPL", response.getBody().symbol());
        verify(crearOrdenUseCase).ejecutar(any(CrearOrdenUseCase.CrearOrdenCommand.class));
    }

    @Test
    void crearOrdenSinTickerLanzaExcepcion() {
        OrdenController.CrearOrdenRequest request = new OrdenController.CrearOrdenRequest(
                null, null, 1L, "COMPRA", "MARKET", "DIA", 10,
                null, null, null, null, null
        );
        AuthenticatedUser usuario = new AuthenticatedUser(1L, Set.of("USER"));

        assertThrows(IllegalArgumentException.class, () -> controller.crearOrden(request, null, usuario));
        verifyNoInteractions(crearOrdenUseCase);
    }

    @Test
    void crearOrdenConIdempotencyKeyHeader() {
        UUID clientOrderId = UUID.randomUUID();
        Orden orden = crearOrden(clientOrderId);

        when(crearOrdenUseCase.ejecutar(any(CrearOrdenUseCase.CrearOrdenCommand.class))).thenReturn(orden);

        OrdenController.CrearOrdenRequest request = new OrdenController.CrearOrdenRequest(
                "AAPL", null, 1L, "COMPRA", "MARKET", "DIA", 10,
                null, null, null, null, null
        );
        AuthenticatedUser usuario = new AuthenticatedUser(1L, Set.of("USER"));
        String headerKey = clientOrderId.toString();

        ResponseEntity<OrdenController.OrdenDetalleResponse> response = controller.crearOrden(request, headerKey, usuario);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(crearOrdenUseCase).ejecutar(argThat(cmd -> cmd.clientOrderId().equals(clientOrderId)));
    }

    @Test
    void crearOrdenConIdempotencyKeyInvalida() {
        UUID clientOrderId = UUID.nameUUIDFromBytes("clave-no-uuid".getBytes());
        Orden orden = crearOrden(clientOrderId);

        when(crearOrdenUseCase.ejecutar(any(CrearOrdenUseCase.CrearOrdenCommand.class))).thenReturn(orden);

        OrdenController.CrearOrdenRequest request = new OrdenController.CrearOrdenRequest(
                "AAPL", null, 1L, "COMPRA", "MARKET", "DIA", 10,
                null, null, null, null, "clave-no-uuid"
        );
        AuthenticatedUser usuario = new AuthenticatedUser(1L, Set.of("USER"));

        ResponseEntity<OrdenController.OrdenDetalleResponse> response = controller.crearOrden(request, null, usuario);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(crearOrdenUseCase).ejecutar(argThat(cmd -> cmd.clientOrderId().equals(clientOrderId)));
    }

    @Test
    void crearOrdenConVigenciaNullDefaultDia() {
        UUID clientOrderId = UUID.randomUUID();
        Orden orden = crearOrden(clientOrderId);

        when(crearOrdenUseCase.ejecutar(any(CrearOrdenUseCase.CrearOrdenCommand.class))).thenReturn(orden);

        OrdenController.CrearOrdenRequest request = new OrdenController.CrearOrdenRequest(
                "AAPL", null, 1L, "COMPRA", "MARKET", null, 10,
                null, null, null, null, null
        );
        AuthenticatedUser usuario = new AuthenticatedUser(1L, Set.of("USER"));

        ResponseEntity<OrdenController.OrdenDetalleResponse> response = controller.crearOrden(request, null, usuario);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(crearOrdenUseCase).ejecutar(argThat(cmd -> cmd.vigenciaOrden() == VigenciaOrden.DIA));
    }

    @Test
    void crearOrdenConPrecioNullUsaPrecioLimite() {
        UUID clientOrderId = UUID.randomUUID();
        Orden orden = crearOrden(clientOrderId);

        when(crearOrdenUseCase.ejecutar(any(CrearOrdenUseCase.CrearOrdenCommand.class))).thenReturn(orden);

        OrdenController.CrearOrdenRequest request = new OrdenController.CrearOrdenRequest(
                "AAPL", null, 1L, "COMPRA", "LIMIT", "DIA", 10,
                null, new BigDecimal("150.00"), null, null, null
        );
        AuthenticatedUser usuario = new AuthenticatedUser(1L, Set.of("USER"));

        ResponseEntity<OrdenController.OrdenDetalleResponse> response = controller.crearOrden(request, null, usuario);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(crearOrdenUseCase).ejecutar(argThat(cmd -> cmd.precioEstimado().compareTo(new BigDecimal("150.00")) == 0));
    }

    @Test
    void crearOrdenConPrecioNullYPrecioLimiteNullUsaPrecioEstimado() {
        UUID clientOrderId = UUID.randomUUID();
        Orden orden = crearOrden(clientOrderId);

        when(crearOrdenUseCase.ejecutar(any(CrearOrdenUseCase.CrearOrdenCommand.class))).thenReturn(orden);

        OrdenController.CrearOrdenRequest request = new OrdenController.CrearOrdenRequest(
                "AAPL", null, 1L, "COMPRA", "MARKET", "DIA", 10,
                null, null, null, new BigDecimal("145.00"), null
        );
        AuthenticatedUser usuario = new AuthenticatedUser(1L, Set.of("USER"));

        ResponseEntity<OrdenController.OrdenDetalleResponse> response = controller.crearOrden(request, null, usuario);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(crearOrdenUseCase).ejecutar(argThat(cmd -> cmd.precioEstimado().compareTo(new BigDecimal("145.00")) == 0));
    }

    @Test
    void crearOrdenConIdempotencyKeyHeaderInvalida() {
        UUID clientOrderId = UUID.nameUUIDFromBytes("header-invalido".getBytes());
        Orden orden = crearOrden(clientOrderId);

        when(crearOrdenUseCase.ejecutar(any(CrearOrdenUseCase.CrearOrdenCommand.class))).thenReturn(orden);

        OrdenController.CrearOrdenRequest request = new OrdenController.CrearOrdenRequest(
                "AAPL", null, 1L, "COMPRA", "MARKET", "DIA", 10,
                null, null, null, null, null
        );
        AuthenticatedUser usuario = new AuthenticatedUser(1L, Set.of("USER"));

        ResponseEntity<OrdenController.OrdenDetalleResponse> response = controller.crearOrden(request, "header-invalido", usuario);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(crearOrdenUseCase).ejecutar(argThat(cmd -> cmd.clientOrderId().equals(clientOrderId)));
    }

    @Test
    void crearOrdenConHeaderBlankBodyValido() {
        UUID clientOrderId = UUID.randomUUID();
        Orden orden = crearOrden(clientOrderId);

        when(crearOrdenUseCase.ejecutar(any(CrearOrdenUseCase.CrearOrdenCommand.class))).thenReturn(orden);

        OrdenController.CrearOrdenRequest request = new OrdenController.CrearOrdenRequest(
                "AAPL", null, 1L, "COMPRA", "MARKET", "DIA", 10,
                null, null, null, null, clientOrderId.toString()
        );
        AuthenticatedUser usuario = new AuthenticatedUser(1L, Set.of("USER"));

        ResponseEntity<OrdenController.OrdenDetalleResponse> response = controller.crearOrden(request, "", usuario);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(crearOrdenUseCase).ejecutar(argThat(cmd -> cmd.clientOrderId().equals(clientOrderId)));
    }

    @Test
    void crearOrdenConHeaderNullBodyValido() {
        UUID clientOrderId = UUID.randomUUID();
        Orden orden = crearOrden(clientOrderId);

        when(crearOrdenUseCase.ejecutar(any(CrearOrdenUseCase.CrearOrdenCommand.class))).thenReturn(orden);

        OrdenController.CrearOrdenRequest request = new OrdenController.CrearOrdenRequest(
                "AAPL", null, 1L, "COMPRA", "MARKET", "DIA", 10,
                null, null, null, null, clientOrderId.toString()
        );
        AuthenticatedUser usuario = new AuthenticatedUser(1L, Set.of("USER"));

        ResponseEntity<OrdenController.OrdenDetalleResponse> response = controller.crearOrden(request, null, usuario);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(crearOrdenUseCase).ejecutar(argThat(cmd -> cmd.clientOrderId().equals(clientOrderId)));
    }

    @Test
    void crearOrdenConHeaderBlankBodyInvalido() {
        UUID clientOrderId = UUID.nameUUIDFromBytes("body-invalido".getBytes());
        Orden orden = crearOrden(clientOrderId);

        when(crearOrdenUseCase.ejecutar(any(CrearOrdenUseCase.CrearOrdenCommand.class))).thenReturn(orden);

        OrdenController.CrearOrdenRequest request = new OrdenController.CrearOrdenRequest(
                "AAPL", null, 1L, "COMPRA", "MARKET", "DIA", 10,
                null, null, null, null, "body-invalido"
        );
        AuthenticatedUser usuario = new AuthenticatedUser(1L, Set.of("USER"));

        ResponseEntity<OrdenController.OrdenDetalleResponse> response = controller.crearOrden(request, "", usuario);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(crearOrdenUseCase).ejecutar(argThat(cmd -> cmd.clientOrderId().equals(clientOrderId)));
    }

    @Test
    void crearOrdenConTickerNullUsaSimbolo() {
        UUID clientOrderId = UUID.randomUUID();
        Orden orden = crearOrden(clientOrderId);

        when(crearOrdenUseCase.ejecutar(any(CrearOrdenUseCase.CrearOrdenCommand.class))).thenReturn(orden);

        OrdenController.CrearOrdenRequest request = new OrdenController.CrearOrdenRequest(
                null, "GOOGL", 1L, "COMPRA", "MARKET", "DIA", 10,
                null, null, null, null, null
        );
        AuthenticatedUser usuario = new AuthenticatedUser(1L, Set.of("USER"));

        ResponseEntity<OrdenController.OrdenDetalleResponse> response = controller.crearOrden(request, null, usuario);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(crearOrdenUseCase).ejecutar(argThat(cmd -> cmd.ticker().equals("GOOGL")));
    }

    @Test
    void crearOrdenConPrecioEstimadoNullPrecioNullUsaPrecioLimite() {
        UUID clientOrderId = UUID.randomUUID();
        Orden orden = crearOrden(clientOrderId);

        when(crearOrdenUseCase.ejecutar(any(CrearOrdenUseCase.CrearOrdenCommand.class))).thenReturn(orden);

        OrdenController.CrearOrdenRequest request = new OrdenController.CrearOrdenRequest(
                "AAPL", null, 1L, "COMPRA", "LIMIT", "DIA", 10,
                null, new BigDecimal("150.00"), null, null, null
        );
        AuthenticatedUser usuario = new AuthenticatedUser(1L, Set.of("USER"));

        ResponseEntity<OrdenController.OrdenDetalleResponse> response = controller.crearOrden(request, null, usuario);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(crearOrdenUseCase).ejecutar(argThat(cmd -> cmd.precioEstimado().compareTo(new BigDecimal("150.00")) == 0));
    }

    @Test
    void crearOrdenConPrecioEstimadoNullPrecioProvistoUsaPrecio() {
        UUID clientOrderId = UUID.randomUUID();
        Orden orden = crearOrden(clientOrderId);

        when(crearOrdenUseCase.ejecutar(any(CrearOrdenUseCase.CrearOrdenCommand.class))).thenReturn(orden);

        OrdenController.CrearOrdenRequest request = new OrdenController.CrearOrdenRequest(
                "AAPL", null, 1L, "COMPRA", "LIMIT", "DIA", 10,
                new BigDecimal("155.00"), new BigDecimal("150.00"), null, null, null
        );
        AuthenticatedUser usuario = new AuthenticatedUser(1L, Set.of("USER"));

        ResponseEntity<OrdenController.OrdenDetalleResponse> response = controller.crearOrden(request, null, usuario);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(crearOrdenUseCase).ejecutar(argThat(cmd -> cmd.precioEstimado().compareTo(new BigDecimal("155.00")) == 0));
    }

    @Test
    void crearOrdenConAccionIdNullDefaultUno() {
        UUID clientOrderId = UUID.randomUUID();
        Orden orden = crearOrden(clientOrderId);

        when(crearOrdenUseCase.ejecutar(any(CrearOrdenUseCase.CrearOrdenCommand.class))).thenReturn(orden);

        OrdenController.CrearOrdenRequest request = new OrdenController.CrearOrdenRequest(
                "AAPL", null, null, "COMPRA", "MARKET", "DIA", 10,
                null, null, null, null, null
        );
        AuthenticatedUser usuario = new AuthenticatedUser(1L, Set.of("USER"));

        ResponseEntity<OrdenController.OrdenDetalleResponse> response = controller.crearOrden(request, null, usuario);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(crearOrdenUseCase).ejecutar(argThat(cmd -> cmd.accionId() == 1L));
    }

    @Test
    void crearOrdenSinIdempotencyKeyGeneraRandom() {
        UUID clientOrderId = UUID.randomUUID();
        Orden orden = crearOrden(clientOrderId);

        when(crearOrdenUseCase.ejecutar(any(CrearOrdenUseCase.CrearOrdenCommand.class))).thenReturn(orden);

        OrdenController.CrearOrdenRequest request = new OrdenController.CrearOrdenRequest(
                "AAPL", null, 1L, "COMPRA", "MARKET", "DIA", 10,
                null, null, null, null, null
        );
        AuthenticatedUser usuario = new AuthenticatedUser(1L, Set.of("USER"));

        ResponseEntity<OrdenController.OrdenDetalleResponse> response = controller.crearOrden(request, null, usuario);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(crearOrdenUseCase).ejecutar(argThat(cmd -> cmd.clientOrderId() != null));
    }

    @Test
    void crearOrdenConTickerYSimboloBlankLanzaExcepcion() {
        OrdenController.CrearOrdenRequest request = new OrdenController.CrearOrdenRequest(
                null, "", 1L, "COMPRA", "MARKET", "DIA", 10,
                null, null, null, null, null
        );
        AuthenticatedUser usuario = new AuthenticatedUser(1L, Set.of("USER"));

        assertThrows(IllegalArgumentException.class, () -> controller.crearOrden(request, null, usuario));
        verifyNoInteractions(crearOrdenUseCase);
    }

    @Test
    void crearOrdenConIdempotencyKeyBlankGeneraRandom() {
        UUID clientOrderId = UUID.randomUUID();
        Orden orden = crearOrden(clientOrderId);

        when(crearOrdenUseCase.ejecutar(any(CrearOrdenUseCase.CrearOrdenCommand.class))).thenReturn(orden);

        OrdenController.CrearOrdenRequest request = new OrdenController.CrearOrdenRequest(
                "AAPL", null, 1L, "COMPRA", "MARKET", "DIA", 10,
                null, null, null, null, ""
        );
        AuthenticatedUser usuario = new AuthenticatedUser(1L, Set.of("USER"));

        ResponseEntity<OrdenController.OrdenDetalleResponse> response = controller.crearOrden(request, "", usuario);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(crearOrdenUseCase).ejecutar(argThat(cmd -> cmd.clientOrderId() != null));
    }

    @Test
    void obtenerOrdenExitoUsuarioPropietario() {
        UUID clientOrderId = UUID.randomUUID();
        Orden orden = crearOrden(clientOrderId);
        orden.setUsuarioId(1L);

        when(consultarOrdenUseCase.buscarPorId(1L)).thenReturn(orden);
        when(consultarOrdenUseCase.buscarFillsPorOrden(1L)).thenReturn(List.of());

        AuthenticatedUser usuario = new AuthenticatedUser(1L, Set.of("USER"));

        ResponseEntity<OrdenController.OrdenDetalleResponse> response = controller.obtenerOrden(1L, usuario);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().id());
        verify(consultarOrdenUseCase).buscarPorId(1L);
    }

    @Test
    void obtenerOrdenUsuarioAjenoNoAdmin() {
        UUID clientOrderId = UUID.randomUUID();
        Orden orden = crearOrden(clientOrderId);
        orden.setUsuarioId(99L);

        when(consultarOrdenUseCase.buscarPorId(1L)).thenReturn(orden);

        AuthenticatedUser usuario = new AuthenticatedUser(1L, Set.of("USER"));

        ResponseEntity<OrdenController.OrdenDetalleResponse> response = controller.obtenerOrden(1L, usuario);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(consultarOrdenUseCase).buscarPorId(1L);
    }

    @Test
    void obtenerOrdenUsuarioAjenoEsAdmin() {
        UUID clientOrderId = UUID.randomUUID();
        Orden orden = crearOrden(clientOrderId);
        orden.setUsuarioId(99L);

        when(consultarOrdenUseCase.buscarPorId(1L)).thenReturn(orden);
        when(consultarOrdenUseCase.buscarFillsPorOrden(1L)).thenReturn(List.of());

        AuthenticatedUser usuario = new AuthenticatedUser(1L, Set.of("ADMINISTRADOR"));

        ResponseEntity<OrdenController.OrdenDetalleResponse> response = controller.obtenerOrden(1L, usuario);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void obtenerOrdenNoExistente() {
        when(consultarOrdenUseCase.buscarPorId(999L)).thenThrow(new IllegalArgumentException("Orden no encontrada"));

        AuthenticatedUser usuario = new AuthenticatedUser(1L, Set.of("USER"));

        assertThrows(IllegalArgumentException.class, () -> controller.obtenerOrden(999L, usuario));
    }

    @Test
    void obtenerOrdenConClientOrderIdNull() {
        UUID clientOrderId = null;
        Orden orden = crearOrden(clientOrderId);
        orden.setUsuarioId(1L);
        orden.setClientOrderId(null);

        when(consultarOrdenUseCase.buscarPorId(1L)).thenReturn(orden);
        when(consultarOrdenUseCase.buscarFillsPorOrden(1L)).thenReturn(List.of());

        AuthenticatedUser usuario = new AuthenticatedUser(1L, Set.of("USER"));

        ResponseEntity<OrdenController.OrdenDetalleResponse> response = controller.obtenerOrden(1L, usuario);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNull(response.getBody().clientOrderId());
        verify(consultarOrdenUseCase).buscarPorId(1L);
    }

    @Test
    void listarOrdenesConFiltros() {
        UUID clientOrderId = UUID.randomUUID();
        Orden orden = crearOrden(clientOrderId);
        orden.setUsuarioId(1L);
        orden.setEstadoOrden(EstadoOrden.PENDIENTE);
        orden.setTicker("AAPL");

        when(consultarOrdenUseCase.buscarPorUsuario(1L, EstadoOrden.PENDIENTE, "AAPL"))
                .thenReturn(List.of(orden));
        when(consultarOrdenUseCase.buscarFillsPorOrden(1L)).thenReturn(List.of());

        AuthenticatedUser usuario = new AuthenticatedUser(1L, Set.of("USER"));

        ResponseEntity<List<OrdenController.OrdenDetalleResponse>> response =
                controller.listarOrdenes("PENDIENTE", "AAPL", usuario);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("AAPL", response.getBody().get(0).symbol());
        verify(consultarOrdenUseCase).buscarPorUsuario(1L, EstadoOrden.PENDIENTE, "AAPL");
    }

    @Test
    void listarOrdenesSinFiltros() {
        UUID clientOrderId = UUID.randomUUID();
        Orden orden = crearOrden(clientOrderId);
        orden.setUsuarioId(1L);

        when(consultarOrdenUseCase.buscarPorUsuario(1L, null, null))
                .thenReturn(List.of(orden));
        when(consultarOrdenUseCase.buscarFillsPorOrden(1L)).thenReturn(List.of());

        AuthenticatedUser usuario = new AuthenticatedUser(1L, Set.of("USER"));

        ResponseEntity<List<OrdenController.OrdenDetalleResponse>> response =
                controller.listarOrdenes(null, null, usuario);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(consultarOrdenUseCase).buscarPorUsuario(1L, null, null);
    }

    @Test
    void listarOrdenesConStatusBlank() {
        UUID clientOrderId = UUID.randomUUID();
        Orden orden = crearOrden(clientOrderId);
        orden.setUsuarioId(1L);

        when(consultarOrdenUseCase.buscarPorUsuario(1L, null, null))
                .thenReturn(List.of(orden));
        when(consultarOrdenUseCase.buscarFillsPorOrden(1L)).thenReturn(List.of());

        AuthenticatedUser usuario = new AuthenticatedUser(1L, Set.of("USER"));

        ResponseEntity<List<OrdenController.OrdenDetalleResponse>> response =
                controller.listarOrdenes("", null, usuario);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(consultarOrdenUseCase).buscarPorUsuario(1L, null, null);
    }

    @Test
    void cancelarOrdenExito() {
        doNothing().when(cancelarOrdenUseCase).ejecutar(1L, 1L, "motivo");

        AuthenticatedUser usuario = new AuthenticatedUser(1L, Set.of("USER"));

        ResponseEntity<Void> response = controller.cancelarOrden(1L, "motivo", usuario);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(cancelarOrdenUseCase).ejecutar(1L, 1L, "motivo");
    }

    @Test
    void cancelarOrdenConMotivoNull() {
        doNothing().when(cancelarOrdenUseCase).ejecutar(1L, 1L, null);

        AuthenticatedUser usuario = new AuthenticatedUser(1L, Set.of("USER"));

        ResponseEntity<Void> response = controller.cancelarOrden(1L, null, usuario);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(cancelarOrdenUseCase).ejecutar(1L, 1L, null);
    }

    @Test
    void cancelarOrdenUsuarioAjeno() {
        doThrow(new IllegalArgumentException("La orden no pertenece al usuario"))
                .when(cancelarOrdenUseCase).ejecutar(1L, 1L, "motivo");

        AuthenticatedUser usuario = new AuthenticatedUser(1L, Set.of("USER"));

        assertThrows(IllegalArgumentException.class, () -> controller.cancelarOrden(1L, "motivo", usuario));
    }

    @Test
    void confirmarEncolamientoExito() {
        UUID clientOrderId = UUID.randomUUID();
        Orden orden = crearOrden(clientOrderId);
        orden.setUsuarioId(1L);
        orden.setRequiereEncolamiento(true);
        orden.confirmarEncolamiento();

        when(confirmarEncolamientoUseCase.ejecutar(1L, 1L)).thenReturn(orden);
        when(consultarOrdenUseCase.buscarFillsPorOrden(1L)).thenReturn(List.of());

        AuthenticatedUser usuario = new AuthenticatedUser(1L, Set.of("USER"));

        ResponseEntity<OrdenController.OrdenDetalleResponse> response = controller.confirmarEncolamiento(1L, usuario);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().requiresQueuing());
        assertNotNull(response.getBody().confirmedQueuingAt());
        verify(confirmarEncolamientoUseCase).ejecutar(1L, 1L);
    }

    @Test
    void consultarFillsExito() {
        UUID clientOrderId = UUID.randomUUID();
        Orden orden = crearOrden(clientOrderId);
        orden.setUsuarioId(1L);

        EjecucionOrden fill = new EjecucionOrden(1L, 1L, "BROKER-123", 5, new BigDecimal("150.00"),
                new BigDecimal("0.30"), Instant.now(), Instant.now());

        when(consultarOrdenUseCase.buscarPorId(1L)).thenReturn(orden);
        when(consultarOrdenUseCase.buscarFillsPorOrden(1L)).thenReturn(List.of(fill));

        AuthenticatedUser usuario = new AuthenticatedUser(1L, Set.of("USER"));

        ResponseEntity<List<EjecucionOrden>> response = controller.consultarFills(1L, usuario);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(5, response.getBody().get(0).getCantidad());
        verify(consultarOrdenUseCase).buscarPorId(1L);
        verify(consultarOrdenUseCase).buscarFillsPorOrden(1L);
    }

    @Test
    void consultarFillsUsuarioAjenoNoAdmin() {
        UUID clientOrderId = UUID.randomUUID();
        Orden orden = crearOrden(clientOrderId);
        orden.setUsuarioId(99L);

        when(consultarOrdenUseCase.buscarPorId(1L)).thenReturn(orden);

        AuthenticatedUser usuario = new AuthenticatedUser(1L, Set.of("USER"));

        ResponseEntity<List<EjecucionOrden>> response = controller.consultarFills(1L, usuario);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(consultarOrdenUseCase).buscarPorId(1L);
        verify(consultarOrdenUseCase, never()).buscarFillsPorOrden(anyLong());
    }

    @Test
    void consultarFillsUsuarioAjenoEsAdmin() {
        UUID clientOrderId = UUID.randomUUID();
        Orden orden = crearOrden(clientOrderId);
        orden.setUsuarioId(99L);

        EjecucionOrden fill = new EjecucionOrden(1L, 1L, "BROKER-123", 5, new BigDecimal("150.00"),
                new BigDecimal("0.30"), Instant.now(), Instant.now());

        when(consultarOrdenUseCase.buscarPorId(1L)).thenReturn(orden);
        when(consultarOrdenUseCase.buscarFillsPorOrden(1L)).thenReturn(List.of(fill));

        AuthenticatedUser usuario = new AuthenticatedUser(1L, Set.of("ADMINISTRADOR"));

        ResponseEntity<List<EjecucionOrden>> response = controller.consultarFills(1L, usuario);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(consultarOrdenUseCase).buscarPorId(1L);
        verify(consultarOrdenUseCase).buscarFillsPorOrden(1L);
    }

    private Orden crearOrden(UUID clientOrderId) {
        Orden orden = new Orden();
        orden.setId(1L);
        orden.setUsuarioId(1L);
        orden.setAccionId(1L);
        orden.setTicker("AAPL");
        orden.setTipoOrden(TipoOrden.MARKET);
        orden.setLadoOrden(LadoOrden.COMPRA);
        orden.setVigenciaOrden(VigenciaOrden.DIA);
        orden.setEstadoOrden(EstadoOrden.PENDIENTE);
        orden.setCantidad(10);
        orden.setPrecioLimite(null);
        orden.setPrecioActivacion(null);
        orden.setPrecioEstimado(new BigDecimal("150.00"));
        orden.setRequiereEncolamiento(false);
        orden.setConfirmadoEncolamientoEn(null);
        orden.setClientOrderId(clientOrderId);
        orden.setBrokerOrderId(null);
        orden.setReservaFondosId(null);
        orden.setReservaPosicionId(null);
        orden.setCreadaEn(Instant.now());
        orden.setActualizadaEn(Instant.now());
        return orden;
    }
}
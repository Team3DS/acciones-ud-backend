package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.in.rest;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.CancelarOrdenUseCase;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.ConfirmarEncolamientoUseCase;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.ConsultarOrdenUseCase;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.CrearOrdenUseCase;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.CrearOrdenUseCase.CrearOrdenCommand;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.*;
import co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Órdenes", description = "Ciclo de vida y gestión de órdenes bursátiles")
public class OrdenController {

    private final CrearOrdenUseCase crearOrdenUseCase;
    private final ConsultarOrdenUseCase consultarOrdenUseCase;
    private final CancelarOrdenUseCase cancelarOrdenUseCase;
    private final ConfirmarEncolamientoUseCase confirmarEncolamientoUseCase;

    public OrdenController(CrearOrdenUseCase crearOrdenUseCase,
                           ConsultarOrdenUseCase consultarOrdenUseCase,
                           CancelarOrdenUseCase cancelarOrdenUseCase,
                           ConfirmarEncolamientoUseCase confirmarEncolamientoUseCase) {
        this.crearOrdenUseCase = crearOrdenUseCase;
        this.consultarOrdenUseCase = consultarOrdenUseCase;
        this.cancelarOrdenUseCase = cancelarOrdenUseCase;
        this.confirmarEncolamientoUseCase = confirmarEncolamientoUseCase;
    }

    @PostMapping
    @Operation(summary = "Crear una nueva orden de compra o venta")
    public ResponseEntity<OrdenDetalleResponse> crearOrden(
            @Valid @RequestBody CrearOrdenRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKeyHeader,
            @AuthenticationPrincipal AuthenticatedUser usuario) {

        UUID clientOrderId = resolverClientOrderId(request.idempotencyKey(), idempotencyKeyHeader);
        String ticker = request.ticker() != null ? request.ticker() : request.simbolo();
        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException("El símbolo/ticker de la acción es obligatorio");
        }

        LadoOrden lado = LadoOrden.valueOf(request.lado().toUpperCase());
        TipoOrden tipo = TipoOrden.valueOf(request.tipo().toUpperCase());
        VigenciaOrden vigencia = request.vigencia() != null
                ? VigenciaOrden.valueOf(request.vigencia().toUpperCase())
                : VigenciaOrden.DIA;

        BigDecimal precioEstimado = request.precioEstimado() != null
                ? request.precioEstimado()
                : (request.precio() != null ? request.precio() : request.precioLimite());

        CrearOrdenCommand comando = new CrearOrdenCommand(
                usuario.userId(),
                request.accionId() != null ? request.accionId() : 1L,
                ticker,
                tipo,
                lado,
                vigencia,
                request.cantidad(),
                request.precioLimite(),
                request.precioActivacion(),
                precioEstimado,
                clientOrderId
        );

        Orden orden = crearOrdenUseCase.ejecutar(comando);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDetalleResponse(orden));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Obtener el detalle de una orden")
    public ResponseEntity<OrdenDetalleResponse> obtenerOrden(
            @PathVariable Long orderId,
            @AuthenticationPrincipal AuthenticatedUser usuario) {
        Orden orden = consultarOrdenUseCase.buscarPorId(orderId);
        if (!orden.getUsuarioId().equals(usuario.userId()) && !usuario.esAdministrador()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(toDetalleResponse(orden));
    }

    @GetMapping
    @Operation(summary = "Listar las órdenes del usuario con filtros opcionales")
    public ResponseEntity<List<OrdenDetalleResponse>> listarOrdenes(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String symbol,
            @AuthenticationPrincipal AuthenticatedUser usuario) {
        EstadoOrden estado = status != null && !status.isBlank() ? EstadoOrden.valueOf(status.toUpperCase()) : null;
        List<Orden> ordenes = consultarOrdenUseCase.buscarPorUsuario(usuario.userId(), estado, symbol);
        List<OrdenDetalleResponse> response = ordenes.stream().map(this::toDetalleResponse).toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "Cancelar una orden")
    public ResponseEntity<Void> cancelarOrden(
            @PathVariable Long orderId,
            @RequestParam(required = false) String motivo,
            @AuthenticationPrincipal AuthenticatedUser usuario) {
        cancelarOrdenUseCase.ejecutar(orderId, usuario.userId(), motivo);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{orderId}/confirm-queue")
    @Operation(summary = "Confirmar explícitamente el encolamiento de una orden creada fuera de horario")
    public ResponseEntity<OrdenDetalleResponse> confirmarEncolamiento(
            @PathVariable Long orderId,
            @AuthenticationPrincipal AuthenticatedUser usuario) {
        Orden orden = confirmarEncolamientoUseCase.ejecutar(orderId, usuario.userId());
        return ResponseEntity.ok(toDetalleResponse(orden));
    }

    @GetMapping("/{orderId}/fills")
    @Operation(summary = "Consultar los fills/ejecuciones de una orden")
    public ResponseEntity<List<EjecucionOrden>> consultarFills(
            @PathVariable Long orderId,
            @AuthenticationPrincipal AuthenticatedUser usuario) {
        Orden orden = consultarOrdenUseCase.buscarPorId(orderId);
        if (!orden.getUsuarioId().equals(usuario.userId()) && !usuario.esAdministrador()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(consultarOrdenUseCase.buscarFillsPorOrden(orderId));
    }

    private UUID resolverClientOrderId(String idempotencyKeyBody, String idempotencyKeyHeader) {
        String key = (idempotencyKeyHeader != null && !idempotencyKeyHeader.isBlank())
                ? idempotencyKeyHeader
                : idempotencyKeyBody;
        if (key != null && !key.isBlank()) {
            try {
                return UUID.fromString(key);
            } catch (IllegalArgumentException ex) {
                return UUID.nameUUIDFromBytes(key.getBytes());
            }
        }
        return UUID.randomUUID();
    }

    private OrdenDetalleResponse toDetalleResponse(Orden o) {
        List<EjecucionOrden> fills = consultarOrdenUseCase.buscarFillsPorOrden(o.getId());
        int ejecutada = o.cantidadEjecutada(fills);
        int remanente = Math.max(0, o.getCantidad() - ejecutada);

        return new OrdenDetalleResponse(
                o.getId(),
                o.getUsuarioId(),
                o.getAccionId(),
                o.getTicker(),
                o.getLadoOrden().name(),
                o.getTipoOrden().name(),
                o.getVigenciaOrden().name(),
                o.getEstadoOrden().name(),
                o.getCantidad(),
                ejecutada,
                remanente,
                o.getPrecioLimite(),
                o.getPrecioActivacion(),
                o.getPrecioEstimado(),
                o.getRequiereEncolamiento(),
                o.getConfirmadoEncolamientoEn(),
                o.getClientOrderId() != null ? o.getClientOrderId().toString() : null,
                o.getBrokerOrderId(),
                o.getReservaFondosId(),
                o.getReservaPosicionId(),
                o.getCreadaEn(),
                o.getActualizadaEn()
        );
    }

    public record CrearOrdenRequest(
            String ticker,
            String simbolo,
            Long accionId,
            @NotNull String lado,
            @NotNull String tipo,
            String vigencia,
            @NotNull @Min(1) Integer cantidad,
            BigDecimal precio,
            BigDecimal precioLimite,
            BigDecimal precioActivacion,
            BigDecimal precioEstimado,
            String idempotencyKey
    ) {}

    public record OrdenDetalleResponse(
            Long id,
            Long usuarioId,
            Long accionId,
            String symbol,
            String side,
            String type,
            String timeInForce,
            String status,
            Integer quantity,
            Integer executedQuantity,
            Integer remainingQuantity,
            BigDecimal limitPrice,
            BigDecimal stopPrice,
            BigDecimal estimatedPrice,
            Boolean requiresQueuing,
            Instant confirmedQueuingAt,
            String clientOrderId,
            String brokerOrderId,
            Long reservationFundsId,
            Long reservationPositionId,
            Instant createdAt,
            Instant updatedAt
    ) {}
}

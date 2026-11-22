package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.in.rest;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.ConsultarEjecucionesUseCase;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.ConsultarOrdenUseCase;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EjecucionOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.Orden;
import co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.format.DateTimeParseException;

/**
 * Ejecuciones del usuario (wiki "Contratos REST y Eventos", RF-65 y RF-66): resumen de ganancia o pérdida bruta
 * realizada y comisiones de compra y venta, y detalle de una ejecución.
 */
@Tag(name = "Ejecuciones", description = "Resumen y detalle de las ejecuciones del usuario")
@RestController
@RequestMapping("/api/v1/executions")
public class EjecucionController {

    private final ConsultarEjecucionesUseCase consultarEjecuciones;
    private final ConsultarOrdenUseCase consultarOrden;

    public EjecucionController(ConsultarEjecucionesUseCase consultarEjecuciones, ConsultarOrdenUseCase consultarOrden) {
        this.consultarEjecuciones = consultarEjecuciones;
        this.consultarOrden = consultarOrden;
    }

    @Operation(summary = "Resumen de ganancia o pérdida bruta realizada y comisiones",
            description = "from y to son opcionales (ISO-8601). Sin ellos se resume todo el historial.")
    @GetMapping("/summary")
    public ConsultarEjecucionesUseCase.ResumenEjecucionesResponse resumen(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @AuthenticationPrincipal AuthenticatedUser usuario) {
        Instant desde = instante(from, "from");
        Instant hasta = instante(to, "to");
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException("El parámetro from no puede ser posterior a to");
        }
        return consultarEjecuciones.resumen(usuario.userId(), desde, hasta);
    }

    @Operation(summary = "Detalle de una ejecución del usuario")
    @GetMapping("/{executionId}")
    public ResponseEntity<EjecucionOrden> detalle(@PathVariable Long executionId,
                                                  @AuthenticationPrincipal AuthenticatedUser usuario) {
        EjecucionOrden ejecucion = consultarEjecuciones.buscarPorId(executionId);
        Orden orden = consultarOrden.buscarPorId(ejecucion.getOrdenId());
        if (!orden.getUsuarioId().equals(usuario.userId()) && !usuario.esAdministrador()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(ejecucion);
    }

    private static Instant instante(String valor, String nombre) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(valor.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("El parámetro " + nombre + " debe tener formato ISO-8601, por ejemplo 2026-11-01T00:00:00Z");
        }
    }
}

package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.in.rest;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.CancelarOrdenUseCase;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.ConsultarOrdenUseCase;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.CrearOrdenUseCase;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.CrearOrdenUseCase.CrearOrdenCommand;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.CrearOrdenUseCase.OrdenResultado;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.Orden;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrdenController {

    private final CrearOrdenUseCase crearOrdenUseCase;
    private final ConsultarOrdenUseCase consultarOrdenUseCase;
    private final CancelarOrdenUseCase cancelarOrdenUseCase;

    public OrdenController(CrearOrdenUseCase crearOrdenUseCase,
                           ConsultarOrdenUseCase consultarOrdenUseCase,
                           CancelarOrdenUseCase cancelarOrdenUseCase) {
        this.crearOrdenUseCase = crearOrdenUseCase;
        this.consultarOrdenUseCase = consultarOrdenUseCase;
        this.cancelarOrdenUseCase = cancelarOrdenUseCase;
    }

    @PostMapping
    public ResponseEntity<OrdenResultado> crearOrden(@Valid @RequestBody CrearOrdenRequest request,
                                                     Principal principal) {
        Long usuarioId = Long.parseLong(principal.getName());
        CrearOrdenCommand comando = new CrearOrdenCommand(
                usuarioId,
                request.simbolo(),
                request.lado(),
                request.tipo(),
                request.cantidad(),
                request.precio(),
                request.idempotencyKey()
        );
        Orden orden = crearOrdenUseCase.ejecutar(comando);
        return ResponseEntity.ok(crearOrdenUseCase.convertir(orden));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrdenResultado> obtenerOrden(@PathVariable Long orderId,
                                                        Principal principal) {
        Long usuarioId = Long.parseLong(principal.getName());
        Orden orden = consultarOrdenUseCase.buscarPorId(orderId);
        if (!orden.getUsuarioId().equals(usuarioId)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(consultarOrdenUseCase.resumen(orderId));
    }

    @GetMapping
    public ResponseEntity<List<OrdenResultado>> listarOrdenes(Principal principal) {
        Long usuarioId = Long.parseLong(principal.getName());
        List<Orden> ordenes = consultarOrdenUseCase.buscarPorUsuario(usuarioId);
        List<OrdenResultado> resultados = ordenes.stream()
                .map(crearOrdenUseCase::convertir)
                .toList();
        return ResponseEntity.ok(resultados);
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelarOrden(@PathVariable Long orderId,
                                               @RequestParam String motivo,
                                               Principal principal) {
        Long usuarioId = Long.parseLong(principal.getName());
        Orden orden = consultarOrdenUseCase.buscarPorId(orderId);
        if (!orden.getUsuarioId().equals(usuarioId)) {
            return ResponseEntity.status(403).build();
        }
        cancelarOrdenUseCase.ejecutar(orderId, motivo);
        return ResponseEntity.noContent().build();
    }

    public record CrearOrdenRequest(
            String simbolo,
            String lado,
            String tipo,
            Integer cantidad,
            java.math.BigDecimal precio,
            String idempotencyKey
    ) {}
}

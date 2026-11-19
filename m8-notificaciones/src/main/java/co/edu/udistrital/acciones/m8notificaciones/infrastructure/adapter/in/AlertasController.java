package co.edu.udistrital.acciones.m8notificaciones.infrastructure.adapter.in;

import co.edu.udistrital.acciones.m8notificaciones.application.service.AlertasService;
import co.edu.udistrital.acciones.m8notificaciones.domain.AlertaPrecio;
import co.edu.udistrital.acciones.m8notificaciones.domain.CondicionPrecio;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts/price")
public class AlertasController {
    private final AlertasService alerts;

    public AlertasController(AlertasService alerts) {
        this.alerts = alerts;
    }

    @PostMapping
    public ResponseEntity<AlertaPrecio> crear(
            Principal principal,
            @RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody CrearAlerta request) {
        AlertaPrecio alert =
                alerts.crear(
                        Long.parseLong(principal.getName()),
                        request.instrumentId(),
                        request.condition(),
                        request.targetPrice(),
                        key);
        return ResponseEntity.created(URI.create("/api/v1/alerts/price/" + alert.id())).body(alert);
    }

    @GetMapping
    public List<AlertaPrecio> listar(Principal principal) {
        return alerts.listar(Long.parseLong(principal.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(Principal principal, @PathVariable long id) {
        alerts.eliminar(Long.parseLong(principal.getName()), id);
        return ResponseEntity.noContent().build();
    }

    public record CrearAlerta(
            @NotNull @Positive Long instrumentId,
            @NotNull CondicionPrecio condition,
            @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 14, fraction = 4)
                    BigDecimal targetPrice) {}
}

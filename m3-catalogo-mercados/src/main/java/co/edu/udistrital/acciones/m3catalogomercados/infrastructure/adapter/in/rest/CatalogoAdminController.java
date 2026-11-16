package co.edu.udistrital.acciones.m3catalogomercados.infrastructure.adapter.in.rest;

import co.edu.udistrital.acciones.m3catalogomercados.application.command.ActualizarInstrumentoCommand;
import co.edu.udistrital.acciones.m3catalogomercados.application.command.ActualizarMercadoCommand;
import co.edu.udistrital.acciones.m3catalogomercados.application.command.CrearInstrumentoCommand;
import co.edu.udistrital.acciones.m3catalogomercados.application.command.CrearMercadoCommand;
import co.edu.udistrital.acciones.m3catalogomercados.application.service.CatalogoAdminService;
import co.edu.udistrital.acciones.m3catalogomercados.domain.ResultadoComando;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CatalogoAdminController {
    private final CatalogoAdminService admin;

    public CatalogoAdminController(CatalogoAdminService admin) {
        this.admin = admin;
    }

    @PostMapping({"/api/v1/catalog/markets", "/api/v1/markets"})
    public ResponseEntity<?> crearMercado(
            @Valid @RequestBody CrearMercadoCommand command,
            @RequestHeader("Idempotency-Key") String key,
            @AuthenticationPrincipal Long actor) {
        return response(admin.crearMercado(command, key, actor));
    }

    @PatchMapping({"/api/v1/catalog/markets/{code}", "/api/v1/markets/{code}"})
    public ResponseEntity<?> actualizarMercado(
            @PathVariable String code,
            @Valid @RequestBody ActualizarMercadoCommand command,
            @RequestHeader("Idempotency-Key") String key,
            @AuthenticationPrincipal Long actor) {
        return response(admin.actualizarMercado(code, command, key, actor));
    }

    @PostMapping("/api/v1/instruments")
    public ResponseEntity<?> crearInstrumento(
            @Valid @RequestBody CrearInstrumentoCommand command,
            @RequestHeader("Idempotency-Key") String key,
            @AuthenticationPrincipal Long actor) {
        return response(admin.crearInstrumento(command, key, actor));
    }

    @PatchMapping("/api/v1/instruments/{symbol}")
    public ResponseEntity<?> actualizarInstrumento(
            @PathVariable String symbol,
            @RequestParam(required = false) String market,
            @Valid @RequestBody ActualizarInstrumentoCommand command,
            @RequestHeader("Idempotency-Key") String key,
            @AuthenticationPrincipal Long actor) {
        return response(admin.actualizarInstrumento(symbol, market, command, key, actor));
    }

    private static <T> ResponseEntity<T> response(ResultadoComando<T> result) {
        return ResponseEntity.status(result.status())
                .header("Location", result.location())
                .header("Idempotency-Replayed", Boolean.toString(result.replayed()))
                .body(result.body());
    }
}

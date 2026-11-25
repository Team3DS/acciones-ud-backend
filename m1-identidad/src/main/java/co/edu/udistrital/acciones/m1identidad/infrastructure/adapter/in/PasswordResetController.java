package co.edu.udistrital.acciones.m1identidad.infrastructure.adapter.in;

import co.edu.udistrital.acciones.m1identidad.application.dto.PasswordResetConfirmRequest;
import co.edu.udistrital.acciones.m1identidad.application.dto.PasswordResetRequest;
import co.edu.udistrital.acciones.m1identidad.application.service.PasswordResetApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth/password-reset")
public class PasswordResetController {
    private final PasswordResetApplicationService service;

    public PasswordResetController(PasswordResetApplicationService service) {
        this.service = service;
    }

    @PostMapping("/request")
    public ResponseEntity<Map<String, String>> request(@Valid @RequestBody PasswordResetRequest request) {
        service.request(request.correo());
        return ResponseEntity.accepted().body(Map.of("message",
                "Si la cuenta existe, recibirás un código para restablecer tu contraseña."));
    }

    @PostMapping("/confirm")
    public ResponseEntity<Map<String, String>> confirm(@Valid @RequestBody PasswordResetConfirmRequest request) {
        service.confirm(request);
        return ResponseEntity.ok(Map.of("message", "Contraseña actualizada. Inicia sesión nuevamente."));
    }
}

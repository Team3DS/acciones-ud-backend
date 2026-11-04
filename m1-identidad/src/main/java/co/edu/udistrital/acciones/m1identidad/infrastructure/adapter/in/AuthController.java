package co.edu.udistrital.acciones.m1identidad.infrastructure.adapter.in;

import co.edu.udistrital.acciones.m1identidad.application.dto.LoginRequest;
import co.edu.udistrital.acciones.m1identidad.application.dto.MfaVerifyRequest;
import co.edu.udistrital.acciones.m1identidad.application.dto.RegisterUserRequest;
import co.edu.udistrital.acciones.m1identidad.application.service.UserApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final UserApplicationService service;
    public AuthController(UserApplicationService service) { this.service = service; }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterUserRequest req) {
        var user = service.register(req);
        return ResponseEntity.ok().body(java.util.Map.of("id", user.getId(), "correo", user.getCorreo()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        var tokens = service.login(req);
        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<?> mfaVerify(@RequestBody MfaVerifyRequest req) {
        // Stub MFA: aceptar código 123456
        boolean ok = "123456".equals(req.code());
        return ResponseEntity.ok().body(java.util.Map.of("verified", ok));
    }
}

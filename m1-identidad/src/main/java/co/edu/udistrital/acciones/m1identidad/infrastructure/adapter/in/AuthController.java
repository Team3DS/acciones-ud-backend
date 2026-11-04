package co.edu.udistrital.acciones.m1identidad.infrastructure.adapter.in;

import co.edu.udistrital.acciones.m1identidad.application.dto.LoginRequest;
import co.edu.udistrital.acciones.m1identidad.application.dto.MfaVerifyRequest;
import co.edu.udistrital.acciones.m1identidad.application.dto.RefreshRequest;
import co.edu.udistrital.acciones.m1identidad.application.dto.RegisterUserRequest;
import co.edu.udistrital.acciones.m1identidad.application.service.UserApplicationService;
import co.edu.udistrital.acciones.m1identidad.infrastructure.service.JwtService;
import co.edu.udistrital.acciones.m1identidad.infrastructure.service.RefreshTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final UserApplicationService service;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    public AuthController(UserApplicationService service, JwtService jwtService, RefreshTokenService refreshTokenService) { 
        this.service = service; 
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

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
        boolean ok = "123456".equals(req.code());
        return ResponseEntity.ok().body(java.util.Map.of("verified", ok));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest req) {
        String stored = refreshTokenService.get(req.userId());
        if (stored == null || !stored.equals(req.refreshToken())) {
            return ResponseEntity.status(401).body(java.util.Map.of("error", "invalid refresh"));
        }
        String newAccess = jwtService.generateAccessToken(req.userId());
        return ResponseEntity.ok().body(java.util.Map.of("accessToken", newAccess));
    }
}

package co.edu.udistrital.acciones.m1identidad.infrastructure.adapter.in;

import co.edu.udistrital.acciones.m1identidad.application.dto.LoginRequest;
import co.edu.udistrital.acciones.m1identidad.application.dto.MfaVerifyRequest;
import co.edu.udistrital.acciones.m1identidad.application.dto.RefreshRequest;
import co.edu.udistrital.acciones.m1identidad.application.dto.RegisterUserRequest;
import co.edu.udistrital.acciones.m1identidad.application.service.UserApplicationService;
import co.edu.udistrital.acciones.m1identidad.infrastructure.service.JwtService;
import co.edu.udistrital.acciones.m1identidad.infrastructure.service.RefreshTokenService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
    public ResponseEntity<?> register(@Valid @RequestBody RegisterUserRequest req) {
        var user = service.register(req);
        return ResponseEntity.ok().body(Map.of("id", user.getId(), "correo", user.getCorreo()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        var tokens = service.login(req);
        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<?> mfaVerify(@RequestBody MfaVerifyRequest req) {
        return ResponseEntity.ok(service.verifyMfa(req.userId(), req.code()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest req) {
        var accessToken = service.refresh(req.userId(), req.refreshToken());
        if (accessToken.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid refresh"));
        }
        return ResponseEntity.ok().body(Map.of("accessToken", accessToken.get()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody RefreshRequest req) {
        if (req.userId() == null || req.userId() <= 0 || req.refreshToken() == null || req.refreshToken().isBlank()) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid refresh"));
        }
        String stored = refreshTokenService.get(req.userId());
        if (stored == null || !stored.equals(req.refreshToken())) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid refresh"));
        }
        service.logout(req.userId());
        return ResponseEntity.ok().body(Map.of("message", "Logged out"));
    }
}

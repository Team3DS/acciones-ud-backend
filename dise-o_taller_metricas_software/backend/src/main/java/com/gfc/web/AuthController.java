package com.gfc.web;

import com.gfc.application.AuthService;
import com.gfc.web.dto.AuthResponse;
import com.gfc.web.dto.LoginRequest;
import com.gfc.web.dto.RegistroRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints publicos de autenticacion: registro, login y logout. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** {@code POST /api/auth/registro} — registra un cliente y devuelve el token (201). */
    @PostMapping("/registro")
    public ResponseEntity<AuthResponse> registro(@RequestBody RegistroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrarCliente(request));
    }

    /** {@code POST /api/auth/login} — autentica por nombreUsuario o correo (200). */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /** {@code POST /api/auth/logout} — sin estado de sesion en el servidor (JWT): el cliente simplemente descarta el token (204). */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }
}

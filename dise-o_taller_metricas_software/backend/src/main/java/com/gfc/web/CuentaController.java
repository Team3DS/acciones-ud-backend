package com.gfc.web;

import com.gfc.application.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cuenta")
public class CuentaController {
    private final AuthService authService;

    public CuentaController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/baja")
    public ResponseEntity<Void> darseDeBaja(Authentication authentication) {
        authService.darseDeBaja(authentication.getName());
        return ResponseEntity.noContent().build();
    }
}

package co.edu.udistrital.acciones.m1identidad.infrastructure.adapter.in;

import co.edu.udistrital.acciones.m1identidad.application.dto.UserInfoResponse;
import co.edu.udistrital.acciones.m1identidad.application.service.UserApplicationService;
import co.edu.udistrital.acciones.m1identidad.infrastructure.service.JwtService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class UserController {

    private final UserApplicationService userService;
    private final JwtService jwtService;

    public UserController(UserApplicationService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }

    @GetMapping("/users/me")
    public ResponseEntity<UserInfoResponse> me(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {
        Long userId = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                userId = jwtService.parseUserId(token);
            } catch (Exception ignored) {
            }
        }
        if (userId == null) {
            userId = headerUserId;
        }
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserInfoResponse response = userService.getUserProfile(userId);
        return ResponseEntity.ok(response);
    }
}
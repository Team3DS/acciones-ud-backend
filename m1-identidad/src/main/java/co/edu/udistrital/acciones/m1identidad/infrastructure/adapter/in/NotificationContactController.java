package co.edu.udistrital.acciones.m1identidad.infrastructure.adapter.in;

import co.edu.udistrital.acciones.m1identidad.application.dto.NotificationContactResponse;
import co.edu.udistrital.acciones.m1identidad.application.service.UserApplicationService;
import co.edu.udistrital.acciones.m1identidad.infrastructure.service.JwtService;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificationContactController {
    private final UserApplicationService users;
    private final JwtService jwt;

    public NotificationContactController(UserApplicationService users, JwtService jwt) {
        this.users = users;
        this.jwt = jwt;
    }

    @GetMapping("/internal/users/{userId}/notification-contact")
    public ResponseEntity<NotificationContactResponse> contact(
            @PathVariable Long userId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            if (!jwt.isNotificationServiceToken(authorization.substring(7))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } catch (JwtException | IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (userId <= 0) {
            return ResponseEntity.badRequest().build();
        }
        return users.notificationEmail(userId)
                .map(email -> ResponseEntity.ok(new NotificationContactResponse(email)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

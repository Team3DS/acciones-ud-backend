package co.edu.udistrital.acciones.m1identidad.infrastructure.adapter.in;

import co.edu.udistrital.acciones.m1identidad.application.port.out.UserRepositoryPort;
import co.edu.udistrital.acciones.m1identidad.application.service.UserApplicationService;
import co.edu.udistrital.acciones.m1identidad.domain.User;
import co.edu.udistrital.acciones.m1identidad.infrastructure.service.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserApplicationService userApplicationService;
    private final UserRepositoryPort userRepository;
    private final JwtService jwtService;

    public UserController(UserApplicationService userApplicationService,
                          UserRepositoryPort userRepository,
                          JwtService jwtService) {
        this.userApplicationService = userApplicationService;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }

    @GetMapping("/me")
    public ResponseEntity<?> getAuthenticatedUser(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Missing or invalid Authorization header"));
        }
        String token = authHeader.substring(7);
        Long userId;
        try {
            userId = jwtService.parseUserId(token);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
        }
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }
        User user = userOpt.get();
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "correo", user.getCorreo(),
                "estadoUsuario", user.getEstadoUsuario()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }
        User user = userOpt.get();
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "correo", user.getCorreo(),
                "estadoUsuario", user.getEstadoUsuario()
        ));
    }

    @GetMapping
    public ResponseEntity<?> listUsers() {
        return ResponseEntity.ok(List.of());
    }

    @PatchMapping("/{id}/roles")
    public ResponseEntity<?> updateUserRoles(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) body.getOrDefault("roles", List.of());
        userApplicationService.assignRoles(id, Set.copyOf(roles));
        return ResponseEntity.ok(Map.of("message", "Roles actualizados"));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> updateUserEstado(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String estado = (String) body.getOrDefault("estado", "");
        String motivo = (String) body.getOrDefault("motivo", "");
        userApplicationService.changeStatus(id, estado, motivo);
        return ResponseEntity.ok(Map.of("message", "Estado actualizado"));
    }
}
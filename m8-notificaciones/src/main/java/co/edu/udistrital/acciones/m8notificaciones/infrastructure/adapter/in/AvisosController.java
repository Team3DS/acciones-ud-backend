package co.edu.udistrital.acciones.m8notificaciones.infrastructure.adapter.in;

import co.edu.udistrital.acciones.m8notificaciones.application.service.NotificacionesService;
import co.edu.udistrital.acciones.m8notificaciones.domain.Canal;
import co.edu.udistrital.acciones.m8notificaciones.domain.Preferencia;
import co.edu.udistrital.acciones.m8notificaciones.domain.TipoAviso;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
public class AvisosController {
    private final NotificacionesService notifications;

    public AvisosController(NotificacionesService notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    public NotificacionesService.Pagina listar(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        return notifications.listar(user(principal), page, size, unreadOnly);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> noLeidas(Principal principal) {
        return Map.of("count", notifications.noLeidas(user(principal)));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> leer(Principal principal, @PathVariable long id) {
        notifications.leer(user(principal), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/preferences")
    public List<Preferencia> preferencias(Principal principal) {
        return notifications.preferencias(user(principal));
    }

    @PatchMapping("/preferences")
    public List<Preferencia> preferencia(
            Principal principal, @Valid @RequestBody PreferenciaRequest request) {
        notifications.preferencia(
                user(principal),
                new Preferencia(request.type(), request.channel(), request.enabled()));
        return notifications.preferencias(user(principal));
    }

    @GetMapping("/markets")
    public List<String> mercados(Principal principal) {
        return notifications.mercados(user(principal));
    }

    @PostMapping("/markets/{market}")
    public ResponseEntity<Void> suscribir(Principal principal, @PathVariable String market) {
        notifications.suscribir(user(principal), market);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/markets/{market}")
    public ResponseEntity<Void> desuscribir(Principal principal, @PathVariable String market) {
        notifications.desuscribir(user(principal), market);
        return ResponseEntity.noContent().build();
    }

    private static long user(Principal principal) {
        return Long.parseLong(principal.getName());
    }

    public record PreferenciaRequest(
            @NotNull TipoAviso type, @NotNull Canal channel, @NotNull Boolean enabled) {}
}

package co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.adapter.in.rest;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.ResultadoAgregado;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.SolicitudWatchlist;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.WatchlistItemResponse;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.usecase.AgregarAWatchlistUseCase;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.usecase.ConsultarWatchlistUseCase;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.usecase.QuitarDeWatchlistUseCase;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/watchlist")
public class WatchlistController {

    private final ConsultarWatchlistUseCase consultar;
    private final AgregarAWatchlistUseCase agregar;
    private final QuitarDeWatchlistUseCase quitar;

    public WatchlistController(ConsultarWatchlistUseCase consultar, AgregarAWatchlistUseCase agregar,
                               QuitarDeWatchlistUseCase quitar) {
        this.consultar = consultar;
        this.agregar = agregar;
        this.quitar = quitar;
    }

    @GetMapping
    public List<WatchlistItemResponse> listar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return consultar.ejecutar(usuario.userId());
    }

    @PostMapping("/items")
    public ResponseEntity<WatchlistItemResponse> agregar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody SolicitudWatchlist solicitud) {
        ResultadoAgregado resultado = agregar.ejecutar(usuario.userId(), solicitud);
        HttpStatus estado = resultado.creado() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(estado)
                .body(WatchlistItemResponse.desde(resultado.item(), null, false));
    }

    @DeleteMapping("/items/{symbol}")
    public ResponseEntity<Void> quitar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                       @PathVariable String symbol) {
        quitar.ejecutar(usuario.userId(), symbol);
        return ResponseEntity.noContent().build();
    }
}

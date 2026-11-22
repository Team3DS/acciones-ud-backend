package co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.adapter.in.rest;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.ResultadoAgregado;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.SolicitudWatchlist;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.WatchlistItemResponse;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.usecase.AgregarAWatchlistUseCase;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.usecase.ConsultarWatchlistUseCase;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.usecase.QuitarDeWatchlistUseCase;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.config.OpenApiConfig;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
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

@Tag(name = "Watchlist", description = "Acciones seguidas por el usuario premium")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER)
@RestController
@RequestMapping("/api/v1/watchlist")
public class WatchlistController {

    private static final String PROBLEMA = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    private final ConsultarWatchlistUseCase consultar;
    private final AgregarAWatchlistUseCase agregar;
    private final QuitarDeWatchlistUseCase quitar;

    public WatchlistController(ConsultarWatchlistUseCase consultar, AgregarAWatchlistUseCase agregar,
                               QuitarDeWatchlistUseCase quitar) {
        this.consultar = consultar;
        this.agregar = agregar;
        this.quitar = quitar;
    }

    @Operation(summary = "Lista la watchlist con el ultimo precio",
            description = "Solo para usuarios premium. Si M4 no responde, conserva el ultimo precio "
                    + "conocido y marca stale=true.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Watchlist del usuario",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = WatchlistItemResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Token ausente o invalido",
                    content = @Content(mediaType = PROBLEMA,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no es premium",
                    content = @Content(mediaType = PROBLEMA,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public List<WatchlistItemResponse> listar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return consultar.ejecutar(usuario.userId());
    }

    @Operation(summary = "Agrega un simbolo a la watchlist",
            description = "M3 valida el instrumento antes de guardarlo. Repetir el simbolo no crea otra fila.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Simbolo agregado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = WatchlistItemResponse.class))),
            @ApiResponse(responseCode = "200", description = "El simbolo ya estaba en la watchlist",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = WatchlistItemResponse.class))),
            @ApiResponse(responseCode = "400", description = "Cuerpo invalido",
                    content = @Content(mediaType = PROBLEMA,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente o invalido",
                    content = @Content(mediaType = PROBLEMA,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no es premium",
                    content = @Content(mediaType = PROBLEMA,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "El instrumento no es valido",
                    content = @Content(mediaType = PROBLEMA,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503", description = "M3 no esta disponible",
                    content = @Content(mediaType = PROBLEMA,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/items")
    public ResponseEntity<WatchlistItemResponse> agregar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody SolicitudWatchlist solicitud) {
        ResultadoAgregado resultado = agregar.ejecutar(usuario.userId(), solicitud);
        HttpStatus estado = resultado.creado() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(estado)
                .body(WatchlistItemResponse.desde(resultado.item(), null, false));
    }

    @Operation(summary = "Quita un simbolo de la watchlist",
            description = "Quitar un simbolo que no estaba no produce error.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Simbolo quitado o inexistente"),
            @ApiResponse(responseCode = "401", description = "Token ausente o invalido",
                    content = @Content(mediaType = PROBLEMA,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no es premium",
                    content = @Content(mediaType = PROBLEMA,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/items/{symbol}")
    public ResponseEntity<Void> quitar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                       @PathVariable String symbol) {
        quitar.ejecutar(usuario.userId(), symbol);
        return ResponseEntity.noContent().build();
    }
}

package co.edu.udistrital.acciones.m6portafolio.infrastructure.adapter.in.rest;

import co.edu.udistrital.acciones.m6portafolio.application.dto.Actor;
import co.edu.udistrital.acciones.m6portafolio.application.dto.PortafolioResponse;
import co.edu.udistrital.acciones.m6portafolio.application.usecase.ConsultarPortafolioUseCase;
import co.edu.udistrital.acciones.m6portafolio.infrastructure.config.OpenApiConfig;
import co.edu.udistrital.acciones.m6portafolio.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Portafolio", description = "Valoracion consolidada del portafolio")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER)
@RestController
@RequestMapping("/api/v1/portfolio")
public class PortafolioController {

    private static final String PROBLEMA = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    private final ConsultarPortafolioUseCase consultarPortafolio;

    public PortafolioController(ConsultarPortafolioUseCase consultarPortafolio) {
        this.consultarPortafolio = consultarPortafolio;
    }

    @Operation(summary = "Consulta el portafolio de un usuario",
            description = "Suma el valor, el costo y el P&L de las posiciones abiertas con el ultimo precio guardado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Portafolio valorado del usuario",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PortafolioResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente o invalido",
                    content = @Content(mediaType = PROBLEMA,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no puede ver ese portafolio",
                    content = @Content(mediaType = PROBLEMA,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{userId}")
    public PortafolioResponse consultar(@PathVariable Long userId,
                                        @AuthenticationPrincipal AuthenticatedUser usuario) {
        return consultarPortafolio.ejecutar(userId,
                new Actor(usuario.userId(), usuario.esAdministrador(), usuario.esSistema()));
    }
}

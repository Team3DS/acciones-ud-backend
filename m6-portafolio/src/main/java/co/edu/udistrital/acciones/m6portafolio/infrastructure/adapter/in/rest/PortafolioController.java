package co.edu.udistrital.acciones.m6portafolio.infrastructure.adapter.in.rest;

import co.edu.udistrital.acciones.m6portafolio.application.dto.Actor;
import co.edu.udistrital.acciones.m6portafolio.application.dto.PortafolioResponse;
import co.edu.udistrital.acciones.m6portafolio.application.usecase.ConsultarPortafolioUseCase;
import co.edu.udistrital.acciones.m6portafolio.infrastructure.config.OpenApiConfig;
import co.edu.udistrital.acciones.m6portafolio.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    private final ConsultarPortafolioUseCase consultarPortafolio;

    public PortafolioController(ConsultarPortafolioUseCase consultarPortafolio) {
        this.consultarPortafolio = consultarPortafolio;
    }

    @Operation(summary = "Consulta el portafolio de un usuario",
            description = "Suma el valor, el costo y el P&L de las posiciones abiertas con el ultimo precio guardado.")
    @GetMapping("/{userId}")
    public PortafolioResponse consultar(@PathVariable Long userId,
                                        @AuthenticationPrincipal AuthenticatedUser usuario) {
        return consultarPortafolio.ejecutar(userId,
                new Actor(usuario.userId(), usuario.esAdministrador(), usuario.esSistema()));
    }
}

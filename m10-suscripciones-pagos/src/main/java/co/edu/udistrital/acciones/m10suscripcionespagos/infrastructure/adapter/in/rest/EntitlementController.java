package co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.adapter.in.rest;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.EntitlementResponse;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.usecase.ConsultarEntitlementUseCase;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Entitlement", description = "Contrato interno para M8")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_TOKEN_INTERNO)
@RestController
@RequestMapping("/internal/subscriptions")
public class EntitlementController {

    private static final String PROBLEMA = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    private final ConsultarEntitlementUseCase consultarEntitlement;

    public EntitlementController(ConsultarEntitlementUseCase consultarEntitlement) {
        this.consultarEntitlement = consultarEntitlement;
    }

    @Operation(summary = "Informa si un usuario es premium",
            description = "Devuelve active y validUntil; active es booleano y validUntil es ISO-8601 UTC o nulo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entitlement del usuario",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = EntitlementResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token interno ausente o invalido",
                    content = @Content(mediaType = PROBLEMA,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{userId}/entitlement")
    public EntitlementResponse entitlement(@PathVariable Long userId) {
        return consultarEntitlement.ejecutar(userId);
    }
}

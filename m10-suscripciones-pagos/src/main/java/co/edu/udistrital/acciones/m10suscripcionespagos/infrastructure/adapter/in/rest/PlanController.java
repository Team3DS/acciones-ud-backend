package co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.adapter.in.rest;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.PlanResponse;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.usecase.ListarPlanesUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Planes", description = "Catalogo de planes disponibles")
@SecurityRequirements
@RestController
@RequestMapping("/api/v1/payments")
public class PlanController {

    private final ListarPlanesUseCase listarPlanes;

    public PlanController(ListarPlanesUseCase listarPlanes) {
        this.listarPlanes = listarPlanes;
    }

    @Operation(summary = "Lista los planes activos",
            description = "Es publico y devuelve los precios mensual y anual del plan premium.")
    @ApiResponse(responseCode = "200", description = "Catalogo de planes",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = PlanResponse.class))))
    @GetMapping("/plans")
    public List<PlanResponse> listar() {
        return listarPlanes.ejecutar();
    }
}

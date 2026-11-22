package co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.adapter.in.rest;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.ReciboResponse;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.usecase.ListarRecibosUseCase;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.config.OpenApiConfig;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Pagos", description = "Recibos del usuario")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER)
@RestController
@RequestMapping("/api/v1/payments/invoices")
public class ReciboController {

    private final ListarRecibosUseCase listarRecibos;

    public ReciboController(ListarRecibosUseCase listarRecibos) {
        this.listarRecibos = listarRecibos;
    }

    @Operation(summary = "Lista los recibos del usuario",
            description = "Devuelve los recibos del usuario autenticado, del mas reciente al mas antiguo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina de recibos"),
            @ApiResponse(responseCode = "401", description = "Token ausente o invalido")
    })
    @GetMapping
    public Page<ReciboResponse> listar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return listarRecibos.ejecutar(usuario.userId(), page, size);
    }
}

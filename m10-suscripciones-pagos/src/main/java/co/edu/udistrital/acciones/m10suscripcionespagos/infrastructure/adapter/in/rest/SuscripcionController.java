package co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.adapter.in.rest;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.CheckoutResponse;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.MiSuscripcionResponse;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.ResultadoCheckout;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.SolicitudSuscripcion;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.usecase.ConsultarMiSuscripcionUseCase;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.usecase.IniciarSuscripcionUseCase;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.config.OpenApiConfig;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Suscripciones", description = "Consulta e inicio de la suscripcion del usuario")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER)
@RestController
@RequestMapping("/api/v1/subscriptions")
public class SuscripcionController {

    private static final String PROBLEMA = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    private final ConsultarMiSuscripcionUseCase consultarMiSuscripcion;
    private final IniciarSuscripcionUseCase iniciarSuscripcion;

    public SuscripcionController(ConsultarMiSuscripcionUseCase consultarMiSuscripcion,
                                 IniciarSuscripcionUseCase iniciarSuscripcion) {
        this.consultarMiSuscripcion = consultarMiSuscripcion;
        this.iniciarSuscripcion = iniciarSuscripcion;
    }

    @Operation(summary = "Inicia la suscripcion premium",
            description = "Crea una sesion de Stripe Checkout o reutiliza la sesion abierta del mismo intervalo.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sesion de checkout creada",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CheckoutResponse.class))),
            @ApiResponse(responseCode = "200", description = "Sesion de checkout reutilizada",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CheckoutResponse.class))),
            @ApiResponse(responseCode = "400", description = "Solicitud invalida",
                    content = @Content(mediaType = PROBLEMA,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente o invalido",
                    content = @Content(mediaType = PROBLEMA,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Suscripcion vigente o clave de idempotencia en conflicto",
                    content = @Content(mediaType = PROBLEMA,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503", description = "Stripe no disponible o catalogo inconsistente",
                    content = @Content(mediaType = PROBLEMA,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<CheckoutResponse> iniciar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Parameter(description = "Clave de idempotencia de 1 a 64 caracteres")
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody SolicitudSuscripcion solicitud) {
        ResultadoCheckout resultado = iniciarSuscripcion.ejecutar(
                usuario.userId(), idempotencyKey, solicitud.interval());
        HttpStatus estado = resultado.reutilizada() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(estado).body(CheckoutResponse.de(resultado.sesion()));
    }

    @Operation(summary = "Consulta la suscripcion del usuario autenticado",
            description = "Indica si el usuario es premium y, si tiene historial, su ultima suscripcion.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado de la suscripcion",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MiSuscripcionResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente o invalido",
                    content = @Content(mediaType = PROBLEMA,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/me")
    public MiSuscripcionResponse miSuscripcion(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return consultarMiSuscripcion.ejecutar(usuario.userId());
    }
}

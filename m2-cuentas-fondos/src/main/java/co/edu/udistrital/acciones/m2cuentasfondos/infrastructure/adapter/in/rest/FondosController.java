package co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.adapter.in.rest;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.Actor;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.DepositoRequest;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.DepositoResponse;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.RetiroRequest;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.RetiroResponse;
import co.edu.udistrital.acciones.m2cuentasfondos.application.usecase.RegistrarDepositoUseCase;
import co.edu.udistrital.acciones.m2cuentasfondos.application.usecase.SolicitarRetiroUseCase;
import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.config.OpenApiConfig;
import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Fondos", description = "Depósitos y retiros sobre la cuenta del usuario")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_SEGURIDAD)
@RestController
@RequestMapping("/api/v1/funds")
@Validated
public class FondosController {

    private static final String CABECERA_IDEMPOTENCIA = "Idempotency-Key";

    private final RegistrarDepositoUseCase registrarDeposito;
    private final SolicitarRetiroUseCase solicitarRetiro;

    public FondosController(RegistrarDepositoUseCase registrarDeposito, SolicitarRetiroUseCase solicitarRetiro) {
        this.registrarDeposito = registrarDeposito;
        this.solicitarRetiro = solicitarRetiro;
    }

    @Operation(summary = "Registra un depósito",
            description = "Acredita fondos a la cuenta del usuario. Requiere rol ADMINISTRADOR.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Depósito registrado"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no puede depositar en esa cuenta",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "La cuenta no existe",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Conflicto de idempotencia",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/deposits")
    @ResponseStatus(HttpStatus.CREATED)
    public DepositoResponse depositar(
            @Parameter(name = CABECERA_IDEMPOTENCIA, in = ParameterIn.HEADER, required = true,
                    description = "Clave única de la operación")
            @RequestHeader(CABECERA_IDEMPOTENCIA) @NotBlank @Size(max = 64) String idempotencyKey,
            @Valid @RequestBody DepositoRequest request,
            @AuthenticationPrincipal AuthenticatedUser usuario) {
        return registrarDeposito.ejecutar(request, idempotencyKey,
                new Actor(usuario.userId(), usuario.esAdministrador()));
    }

    @Operation(summary = "Registra un retiro",
            description = "Debita fondos disponibles de la cuenta del usuario.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Retiro registrado"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no puede retirar de esa cuenta",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "La cuenta no existe",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Conflicto de idempotencia",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Fondos insuficientes",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/withdrawals")
    @ResponseStatus(HttpStatus.CREATED)
    public RetiroResponse retirar(
            @Parameter(name = CABECERA_IDEMPOTENCIA, in = ParameterIn.HEADER, required = true,
                    description = "Clave única de la operación")
            @RequestHeader(CABECERA_IDEMPOTENCIA) @NotBlank @Size(max = 64) String idempotencyKey,
            @Valid @RequestBody RetiroRequest request,
            @AuthenticationPrincipal AuthenticatedUser usuario) {
        return solicitarRetiro.ejecutar(request, idempotencyKey,
                new Actor(usuario.userId(), usuario.esAdministrador()));
    }
}

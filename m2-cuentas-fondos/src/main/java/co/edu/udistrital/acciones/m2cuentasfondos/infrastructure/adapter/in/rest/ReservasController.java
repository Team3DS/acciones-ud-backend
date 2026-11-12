package co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.adapter.in.rest;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.Actor;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.ConfirmarReservaRequest;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.LiberarReservaRequest;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.ReservaRequest;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.ReservaResponse;
import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.SolicitudInvalidaException;
import co.edu.udistrital.acciones.m2cuentasfondos.application.usecase.ConfirmarReservaUseCase;
import co.edu.udistrital.acciones.m2cuentasfondos.application.usecase.ConsultarReservaUseCase;
import co.edu.udistrital.acciones.m2cuentasfondos.application.usecase.LiberarReservaUseCase;
import co.edu.udistrital.acciones.m2cuentasfondos.application.usecase.ReservarFondosUseCase;
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
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Reservas", description = "Reserva, confirmación y liberación de fondos")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_SEGURIDAD)
@RestController
@RequestMapping("/api/v1/funds/reservations")
@Validated
public class ReservasController {

    private static final String CABECERA_IDEMPOTENCIA = "Idempotency-Key";

    private final ReservarFondosUseCase reservarFondos;
    private final ConsultarReservaUseCase consultarReserva;
    private final ConfirmarReservaUseCase confirmarReserva;
    private final LiberarReservaUseCase liberarReserva;

    public ReservasController(ReservarFondosUseCase reservarFondos, ConsultarReservaUseCase consultarReserva,
                              ConfirmarReservaUseCase confirmarReserva, LiberarReservaUseCase liberarReserva) {
        this.reservarFondos = reservarFondos;
        this.consultarReserva = consultarReserva;
        this.confirmarReserva = confirmarReserva;
        this.liberarReserva = liberarReserva;
    }

    @Operation(summary = "Reserva fondos para una orden",
            description = "Bloquea el monto solicitado en el saldo disponible del usuario.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reserva creada"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida o clave de idempotencia ausente",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no puede reservar en esa cuenta",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "La cuenta no existe",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "La orden ya tiene una reserva",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Fondos insuficientes",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaResponse reservar(
            @Parameter(name = CABECERA_IDEMPOTENCIA, in = ParameterIn.HEADER, required = false,
                    description = "Clave única de la operación; también puede venir en el cuerpo")
            @RequestHeader(name = CABECERA_IDEMPOTENCIA, required = false) @Size(max = 64) String claveCabecera,
            @Valid @RequestBody ReservaRequest request,
            @AuthenticationPrincipal AuthenticatedUser usuario) {
        String clave = resolverClave(request.idempotencyKey(), claveCabecera);
        return reservarFondos.ejecutar(request, clave,
                new Actor(usuario.userId(), usuario.esAdministrador()));
    }

    @Operation(summary = "Consulta una reserva",
            description = "Devuelve el estado y los saldos asociados a la reserva.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva consultada"),
            @ApiResponse(responseCode = "403", description = "El usuario no puede consultar esa reserva",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "La reserva no existe",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")
    public ReservaResponse consultar(@PathVariable Long id,
                                     @AuthenticationPrincipal AuthenticatedUser usuario) {
        return consultarReserva.ejecutar(id, new Actor(usuario.userId(), usuario.esAdministrador()));
    }

    @Operation(summary = "Confirma una reserva",
            description = "Aplica la ejecución de la orden y debita los fondos reservados.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva confirmada"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no puede confirmar esa reserva",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "La reserva no existe",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "La reserva no está pendiente",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Fondos insuficientes",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{id}/confirm")
    public ReservaResponse confirmar(@PathVariable Long id,
                                     @Valid @RequestBody ConfirmarReservaRequest request,
                                     @AuthenticationPrincipal AuthenticatedUser usuario) {
        return confirmarReserva.ejecutar(id, request,
                new Actor(usuario.userId(), usuario.esAdministrador()));
    }

    @Operation(summary = "Libera una reserva",
            description = "Devuelve los fondos reservados al saldo disponible.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva liberada"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no puede liberar esa reserva",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "La reserva no existe",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "La reserva no está pendiente",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{id}/release")
    public ReservaResponse liberar(@PathVariable Long id,
                                   @Valid @RequestBody LiberarReservaRequest request,
                                   @AuthenticationPrincipal AuthenticatedUser usuario) {
        return liberarReserva.ejecutar(id, request,
                new Actor(usuario.userId(), usuario.esAdministrador()));
    }

    private static String resolverClave(String claveCuerpo, String claveCabecera) {
        boolean hayCuerpo = claveCuerpo != null && !claveCuerpo.isBlank();
        boolean hayCabecera = claveCabecera != null && !claveCabecera.isBlank();
        if (hayCuerpo && hayCabecera && !claveCuerpo.equals(claveCabecera)) {
            throw new SolicitudInvalidaException(
                    "La clave de idempotencia del cuerpo y de la cabecera no coinciden");
        }
        if (hayCuerpo) {
            return claveCuerpo;
        }
        if (hayCabecera) {
            return claveCabecera;
        }
        throw new SolicitudInvalidaException("La clave de idempotencia es obligatoria");
    }
}

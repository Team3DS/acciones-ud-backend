package co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.adapter.in.rest;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.Actor;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.FiltroMovimientos;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.MovimientoResponse;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.SaldoResponse;
import co.edu.udistrital.acciones.m2cuentasfondos.application.usecase.ConsultarSaldoUseCase;
import co.edu.udistrital.acciones.m2cuentasfondos.application.usecase.ListarMovimientosUseCase;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.TipoMovimiento;
import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.config.OpenApiConfig;
import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@Tag(name = "Cuentas", description = "Consulta de saldo e historial de movimientos")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_SEGURIDAD)
@RestController
@RequestMapping("/api/v1/accounts")
public class CuentasController {

    private final ConsultarSaldoUseCase consultarSaldo;
    private final ListarMovimientosUseCase listarMovimientos;

    public CuentasController(ConsultarSaldoUseCase consultarSaldo, ListarMovimientosUseCase listarMovimientos) {
        this.consultarSaldo = consultarSaldo;
        this.listarMovimientos = listarMovimientos;
    }

    @Operation(summary = "Consulta el saldo de una cuenta",
            description = "Devuelve los saldos contable, disponible y reservado del usuario indicado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Saldo consultado"),
            @ApiResponse(responseCode = "403", description = "El usuario no puede consultar esa cuenta",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "La cuenta no existe",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{userId}/balance")
    public SaldoResponse saldo(@PathVariable Long userId,
                               @AuthenticationPrincipal AuthenticatedUser usuario) {
        return consultarSaldo.ejecutar(userId, new Actor(usuario.userId(), usuario.esAdministrador()));
    }

    @Operation(summary = "Lista los movimientos de una cuenta",
            description = "Historial paginado con filtros opcionales por tipo y rango de fechas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Movimientos consultados",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = MovimientoResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Filtro, orden o paginación inválidos",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no puede consultar esa cuenta",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "La cuenta no existe",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{userId}/movements")
    public Page<MovimientoResponse> movimientos(
            @PathVariable Long userId,
            @RequestParam(name = "filter[tipo]", required = false) TipoMovimiento tipo,
            @RequestParam(name = "filter[desde]", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
            @RequestParam(name = "filter[hasta]", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser usuario) {
        return listarMovimientos.ejecutar(userId, new FiltroMovimientos(tipo, desde, hasta), pageable,
                new Actor(usuario.userId(), usuario.esAdministrador()));
    }
}

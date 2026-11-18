package co.edu.udistrital.acciones.m6portafolio.infrastructure.adapter.in.rest;

import co.edu.udistrital.acciones.m6portafolio.application.dto.Actor;
import co.edu.udistrital.acciones.m6portafolio.application.dto.EjecucionRequest;
import co.edu.udistrital.acciones.m6portafolio.application.dto.EjecucionResponse;
import co.edu.udistrital.acciones.m6portafolio.application.dto.LiberarReservaRequest;
import co.edu.udistrital.acciones.m6portafolio.application.dto.PosicionResponse;
import co.edu.udistrital.acciones.m6portafolio.application.dto.ReservaPosicionResponse;
import co.edu.udistrital.acciones.m6portafolio.application.dto.ReservaRequest;
import co.edu.udistrital.acciones.m6portafolio.application.exception.SolicitudInvalidaException;
import co.edu.udistrital.acciones.m6portafolio.application.usecase.AplicarEjecucionUseCase;
import co.edu.udistrital.acciones.m6portafolio.application.usecase.ConsultarPosicionUseCase;
import co.edu.udistrital.acciones.m6portafolio.application.usecase.LiberarReservaUseCase;
import co.edu.udistrital.acciones.m6portafolio.application.usecase.ListarPosicionesUseCase;
import co.edu.udistrital.acciones.m6portafolio.application.usecase.ReservarAccionesUseCase;
import co.edu.udistrital.acciones.m6portafolio.infrastructure.config.OpenApiConfig;
import co.edu.udistrital.acciones.m6portafolio.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Posiciones", description = "Aplicacion de ejecuciones y consulta de posiciones")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER)
@RestController
@RequestMapping("/api/v1/positions")
public class PosicionController {

    public static final String CLAVE_IDEMPOTENCIA = "Idempotency-Key";
    private static final int LONGITUD_MAXIMA_CLAVE = 64;

    private final AplicarEjecucionUseCase aplicarEjecucion;
    private final ConsultarPosicionUseCase consultarPosicion;
    private final ListarPosicionesUseCase listarPosiciones;
    private final ReservarAccionesUseCase reservarAcciones;
    private final LiberarReservaUseCase liberarReserva;

    public PosicionController(AplicarEjecucionUseCase aplicarEjecucion,
                              ConsultarPosicionUseCase consultarPosicion,
                              ListarPosicionesUseCase listarPosiciones,
                              ReservarAccionesUseCase reservarAcciones,
                              LiberarReservaUseCase liberarReserva) {
        this.aplicarEjecucion = aplicarEjecucion;
        this.consultarPosicion = consultarPosicion;
        this.listarPosiciones = listarPosiciones;
        this.reservarAcciones = reservarAcciones;
        this.liberarReserva = liberarReserva;
    }

    @Operation(summary = "Aplica una ejecucion de compra o venta",
            description = "Una compra actualiza la cantidad y el costo promedio; una compra repetida no aplica cambios.")
    @PostMapping("/updates")
    public EjecucionResponse actualizar(@Valid @RequestBody EjecucionRequest peticion,
                                        @AuthenticationPrincipal AuthenticatedUser usuario) {
        return aplicarEjecucion.ejecutar(peticion, actor(usuario));
    }

    @Operation(summary = "Reserva acciones para una orden de venta",
            description = "Bloquea el disponible de la posicion y registra la reserva en estado PENDING.")
    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaPosicionResponse reservar(
            @Valid @RequestBody ReservaRequest peticion,
            @RequestHeader(value = CLAVE_IDEMPOTENCIA, required = false) String claveCabecera,
            @AuthenticationPrincipal AuthenticatedUser usuario) {
        return reservarAcciones.ejecutar(peticion,
                resolverClave(claveCabecera, peticion.idempotencyKey()), actor(usuario));
    }

    @Operation(summary = "Libera las acciones reservadas de una orden",
            description = "Una liberacion repetida responde 200 sin cambios; una reserva confirmada responde 409.")
    @PostMapping("/reservations/{id}/release")
    public ReservaPosicionResponse liberar(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) LiberarReservaRequest peticion,
            @AuthenticationPrincipal AuthenticatedUser usuario) {
        String motivo = peticion == null ? null : peticion.reason();
        return liberarReserva.ejecutar(id, motivo, actor(usuario));
    }

    @Operation(summary = "Lista las posiciones abiertas de un usuario")
    @GetMapping("/{userId}")
    public List<PosicionResponse> listar(@PathVariable Long userId,
                                         @AuthenticationPrincipal AuthenticatedUser usuario) {
        return listarPosiciones.ejecutar(userId, actor(usuario));
    }

    @Operation(summary = "Consulta una posicion por simbolo")
    @GetMapping("/{userId}/{symbol}")
    public PosicionResponse consultar(@PathVariable Long userId, @PathVariable String symbol,
                                      @AuthenticationPrincipal AuthenticatedUser usuario) {
        return consultarPosicion.ejecutar(userId, symbol, actor(usuario));
    }

    private static Actor actor(AuthenticatedUser usuario) {
        return new Actor(usuario.userId(), usuario.esAdministrador(), usuario.esSistema());
    }

    private static String resolverClave(String cabecera, String cuerpo) {
        boolean conCabecera = cabecera != null && !cabecera.isBlank();
        boolean conCuerpo = cuerpo != null && !cuerpo.isBlank();
        if (conCabecera && conCuerpo && !cabecera.equals(cuerpo)) {
            throw new SolicitudInvalidaException(
                    "La clave de idempotencia del header y del cuerpo no coinciden");
        }
        if (!conCabecera && !conCuerpo) {
            throw new SolicitudInvalidaException("La clave de idempotencia es obligatoria");
        }
        String clave = conCabecera ? cabecera : cuerpo;
        if (clave.length() > LONGITUD_MAXIMA_CLAVE) {
            throw new SolicitudInvalidaException(
                    "La clave de idempotencia no puede superar " + LONGITUD_MAXIMA_CLAVE + " caracteres");
        }
        return clave;
    }
}

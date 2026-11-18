package co.edu.udistrital.acciones.m6portafolio.infrastructure.adapter.in.rest;

import co.edu.udistrital.acciones.m6portafolio.application.dto.Actor;
import co.edu.udistrital.acciones.m6portafolio.application.dto.EjecucionRequest;
import co.edu.udistrital.acciones.m6portafolio.application.dto.EjecucionResponse;
import co.edu.udistrital.acciones.m6portafolio.application.dto.PosicionResponse;
import co.edu.udistrital.acciones.m6portafolio.application.usecase.AplicarEjecucionUseCase;
import co.edu.udistrital.acciones.m6portafolio.application.usecase.ConsultarPosicionUseCase;
import co.edu.udistrital.acciones.m6portafolio.application.usecase.ListarPosicionesUseCase;
import co.edu.udistrital.acciones.m6portafolio.infrastructure.config.OpenApiConfig;
import co.edu.udistrital.acciones.m6portafolio.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Posiciones", description = "Aplicacion de ejecuciones y consulta de posiciones")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER)
@RestController
@RequestMapping("/api/v1/positions")
public class PosicionController {

    private final AplicarEjecucionUseCase aplicarEjecucion;
    private final ConsultarPosicionUseCase consultarPosicion;
    private final ListarPosicionesUseCase listarPosiciones;

    public PosicionController(AplicarEjecucionUseCase aplicarEjecucion,
                              ConsultarPosicionUseCase consultarPosicion,
                              ListarPosicionesUseCase listarPosiciones) {
        this.aplicarEjecucion = aplicarEjecucion;
        this.consultarPosicion = consultarPosicion;
        this.listarPosiciones = listarPosiciones;
    }

    @Operation(summary = "Aplica una ejecucion de compra o venta",
            description = "Una compra actualiza la cantidad y el costo promedio; una compra repetida no aplica cambios.")
    @PostMapping("/updates")
    public EjecucionResponse actualizar(@Valid @RequestBody EjecucionRequest peticion,
                                        @AuthenticationPrincipal AuthenticatedUser usuario) {
        return aplicarEjecucion.ejecutar(peticion, actor(usuario));
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
}

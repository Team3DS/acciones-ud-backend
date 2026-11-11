package co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.adapter.in.rest;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.Actor;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.ReservaRequest;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.ReservaResponse;
import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.SolicitudInvalidaException;
import co.edu.udistrital.acciones.m2cuentasfondos.application.usecase.ConsultarReservaUseCase;
import co.edu.udistrital.acciones.m2cuentasfondos.application.usecase.ReservarFondosUseCase;
import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
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

@RestController
@RequestMapping("/api/v1/funds/reservations")
@Validated
public class ReservasController {

    private final ReservarFondosUseCase reservarFondos;
    private final ConsultarReservaUseCase consultarReserva;

    public ReservasController(ReservarFondosUseCase reservarFondos, ConsultarReservaUseCase consultarReserva) {
        this.reservarFondos = reservarFondos;
        this.consultarReserva = consultarReserva;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaResponse reservar(
            @RequestHeader(name = "Idempotency-Key", required = false) @Size(max = 64) String claveCabecera,
            @Valid @RequestBody ReservaRequest request,
            @AuthenticationPrincipal AuthenticatedUser usuario) {
        String clave = resolverClave(request.idempotencyKey(), claveCabecera);
        return reservarFondos.ejecutar(request, clave,
                new Actor(usuario.userId(), usuario.esAdministrador()));
    }

    @GetMapping("/{id}")
    public ReservaResponse consultar(@PathVariable Long id,
                                     @AuthenticationPrincipal AuthenticatedUser usuario) {
        return consultarReserva.ejecutar(id, new Actor(usuario.userId(), usuario.esAdministrador()));
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

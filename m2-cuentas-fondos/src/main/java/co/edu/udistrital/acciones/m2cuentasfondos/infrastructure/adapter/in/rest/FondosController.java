package co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.adapter.in.rest;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.Actor;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.DepositoRequest;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.DepositoResponse;
import co.edu.udistrital.acciones.m2cuentasfondos.application.usecase.RegistrarDepositoUseCase;
import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/funds")
@Validated
public class FondosController {

    private final RegistrarDepositoUseCase registrarDeposito;

    public FondosController(RegistrarDepositoUseCase registrarDeposito) {
        this.registrarDeposito = registrarDeposito;
    }

    @PostMapping("/deposits")
    @ResponseStatus(HttpStatus.CREATED)
    public DepositoResponse depositar(
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 64) String idempotencyKey,
            @Valid @RequestBody DepositoRequest request,
            @AuthenticationPrincipal AuthenticatedUser usuario) {
        return registrarDeposito.ejecutar(request, idempotencyKey,
                new Actor(usuario.userId(), usuario.esAdministrador()));
    }
}

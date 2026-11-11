package co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.adapter.in.rest;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.Actor;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.SaldoResponse;
import co.edu.udistrital.acciones.m2cuentasfondos.application.usecase.ConsultarSaldoUseCase;
import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
public class CuentasController {

    private final ConsultarSaldoUseCase consultarSaldo;

    public CuentasController(ConsultarSaldoUseCase consultarSaldo) {
        this.consultarSaldo = consultarSaldo;
    }

    @GetMapping("/{userId}/balance")
    public SaldoResponse saldo(@PathVariable Long userId,
                               @AuthenticationPrincipal AuthenticatedUser usuario) {
        return consultarSaldo.ejecutar(userId, new Actor(usuario.userId(), usuario.esAdministrador()));
    }
}

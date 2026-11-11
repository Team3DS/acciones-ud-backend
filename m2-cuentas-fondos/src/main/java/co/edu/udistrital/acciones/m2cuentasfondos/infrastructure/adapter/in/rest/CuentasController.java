package co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.adapter.in.rest;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.Actor;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.FiltroMovimientos;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.MovimientoResponse;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.SaldoResponse;
import co.edu.udistrital.acciones.m2cuentasfondos.application.usecase.ConsultarSaldoUseCase;
import co.edu.udistrital.acciones.m2cuentasfondos.application.usecase.ListarMovimientosUseCase;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.TipoMovimiento;
import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.security.AuthenticatedUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/accounts")
public class CuentasController {

    private final ConsultarSaldoUseCase consultarSaldo;
    private final ListarMovimientosUseCase listarMovimientos;

    public CuentasController(ConsultarSaldoUseCase consultarSaldo, ListarMovimientosUseCase listarMovimientos) {
        this.consultarSaldo = consultarSaldo;
        this.listarMovimientos = listarMovimientos;
    }

    @GetMapping("/{userId}/balance")
    public SaldoResponse saldo(@PathVariable Long userId,
                               @AuthenticationPrincipal AuthenticatedUser usuario) {
        return consultarSaldo.ejecutar(userId, new Actor(usuario.userId(), usuario.esAdministrador()));
    }

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

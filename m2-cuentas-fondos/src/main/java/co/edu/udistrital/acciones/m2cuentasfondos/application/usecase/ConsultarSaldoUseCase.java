package co.edu.udistrital.acciones.m2cuentasfondos.application.usecase;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.Actor;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.SaldoResponse;
import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.AccesoDenegadoException;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.CuentaRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Cuenta;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.MonetaryUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultarSaldoUseCase {

    public static final String MONEDA = "USD";

    private final CuentaRepositoryPort cuentas;

    public ConsultarSaldoUseCase(CuentaRepositoryPort cuentas) {
        this.cuentas = cuentas;
    }

    @Transactional(readOnly = true)
    public SaldoResponse ejecutar(Long userId, Actor actor) {
        if (!actor.puedeVer(userId)) {
            throw new AccesoDenegadoException("No tiene permiso para consultar esta cuenta");
        }
        return cuentas.findByUserId(userId)
                .map(this::respuesta)
                .orElseGet(() -> new SaldoResponse(userId, MonetaryUtil.cero(), MonetaryUtil.cero(),
                        MonetaryUtil.cero(), MONEDA));
    }

    private SaldoResponse respuesta(Cuenta cuenta) {
        return new SaldoResponse(cuenta.getUserId(), cuenta.getSaldoDisponible(),
                cuenta.getSaldoReservado(), cuenta.getSaldoContable(), MONEDA);
    }
}

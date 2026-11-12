package co.edu.udistrital.acciones.m2cuentasfondos.application.usecase;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.OrdenEjecutadaPayload;
import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.RecursoNoEncontradoException;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.CuentaRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.MovimientoRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.service.LibroMovimientos;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Cuenta;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.MonetaryUtil;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.TipoMovimiento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

@Service
public class AcreditarVentaUseCase {

    public static final String PREFIJO_CLAVE = "VTA:";

    private final CuentaRepositoryPort cuentas;
    private final MovimientoRepositoryPort movimientos;
    private final LibroMovimientos libro;
    private final Clock clock;

    public AcreditarVentaUseCase(CuentaRepositoryPort cuentas, MovimientoRepositoryPort movimientos,
                                 LibroMovimientos libro, Clock clock) {
        this.cuentas = cuentas;
        this.movimientos = movimientos;
        this.libro = libro;
        this.clock = clock;
    }

    @Transactional
    public void ejecutar(OrdenEjecutadaPayload payload) {
        BigDecimal monto = MonetaryUtil.validarPositivo(payload.totalAmount());
        String clave = PREFIJO_CLAVE + payload.orderId();
        Instant ahora = Instant.now(clock);

        if (movimientos.findByIdempotencyKey(clave).isPresent()) {
            return;
        }

        cuentas.crearSiNoExiste(payload.userId(), ahora);
        Cuenta cuenta = cuentas.findByUserIdForUpdate(payload.userId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la cuenta del usuario " + payload.userId()));

        if (movimientos.findByIdempotencyKey(clave).isPresent()) {
            return;
        }

        cuenta.abonar(monto, ahora);
        cuentas.save(cuenta);

        libro.registrar(cuenta, TipoMovimiento.CREDITO_VENTA, monto, TipoMovimiento.CREDITO_VENTA.name(), clave,
                "Crédito por la venta de la orden " + payload.orderId());
    }
}

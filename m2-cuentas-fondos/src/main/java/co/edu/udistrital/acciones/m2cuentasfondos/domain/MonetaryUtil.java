package co.edu.udistrital.acciones.m2cuentasfondos.domain;

import co.edu.udistrital.acciones.m2cuentasfondos.domain.excepcion.MontoInvalidoException;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MonetaryUtil {

    public static final int ESCALA = 2;
    public static final RoundingMode REDONDEO = RoundingMode.HALF_UP;

    private MonetaryUtil() {
    }

    public static BigDecimal cero() {
        return BigDecimal.ZERO.setScale(ESCALA, REDONDEO);
    }

    public static BigDecimal normalizar(BigDecimal monto) {
        if (monto == null) {
            throw new MontoInvalidoException("El monto es obligatorio");
        }
        return monto.setScale(ESCALA, REDONDEO);
    }

    public static BigDecimal validarPositivo(BigDecimal monto) {
        BigDecimal normalizado = normalizar(monto);
        if (normalizado.signum() <= 0) {
            throw new MontoInvalidoException("El monto debe ser mayor que cero");
        }
        return normalizado;
    }
}

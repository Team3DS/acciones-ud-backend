package co.edu.udistrital.acciones.m6portafolio.domain;

import co.edu.udistrital.acciones.m6portafolio.domain.excepcion.PrecioInvalidoException;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Montos {

    public static final int ESCALA = 4;
    public static final RoundingMode REDONDEO = RoundingMode.HALF_UP;

    private Montos() {
    }

    public static BigDecimal cero() {
        return BigDecimal.ZERO.setScale(ESCALA, REDONDEO);
    }

    public static BigDecimal normalizar(BigDecimal monto) {
        if (monto == null) {
            throw new PrecioInvalidoException("El precio es obligatorio");
        }
        return monto.setScale(ESCALA, REDONDEO);
    }

    public static BigDecimal validarPositivo(BigDecimal monto) {
        BigDecimal normalizado = normalizar(monto);
        if (normalizado.signum() <= 0) {
            throw new PrecioInvalidoException("El precio debe ser mayor que cero");
        }
        return normalizado;
    }
}

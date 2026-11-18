package co.edu.udistrital.acciones.m6portafolio.domain;

import co.edu.udistrital.acciones.m6portafolio.domain.excepcion.CantidadInvalidaException;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Cantidades {

    public static final int ESCALA = 8;
    public static final RoundingMode REDONDEO = RoundingMode.HALF_UP;

    private Cantidades() {
    }

    public static BigDecimal cero() {
        return BigDecimal.ZERO.setScale(ESCALA, REDONDEO);
    }

    public static BigDecimal normalizar(BigDecimal cantidad) {
        if (cantidad == null) {
            throw new CantidadInvalidaException("La cantidad es obligatoria");
        }
        return cantidad.setScale(ESCALA, REDONDEO);
    }

    public static BigDecimal validarPositiva(BigDecimal cantidad) {
        BigDecimal normalizada = normalizar(cantidad);
        if (normalizada.signum() <= 0) {
            throw new CantidadInvalidaException("La cantidad debe ser mayor que cero");
        }
        return normalizada;
    }
}

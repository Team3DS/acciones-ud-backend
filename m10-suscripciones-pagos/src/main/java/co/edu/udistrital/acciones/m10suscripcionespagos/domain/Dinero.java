package co.edu.udistrital.acciones.m10suscripcionespagos.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Dinero(BigDecimal monto, String moneda) {

    public static final int ESCALA = 2;
    public static final RoundingMode REDONDEO = RoundingMode.HALF_UP;
    public static final String USD = "USD";

    public Dinero {
        Objects.requireNonNull(monto, "El monto es obligatorio");
        String codigo = moneda == null || moneda.isBlank() ? USD : moneda.strip().toUpperCase();
        if (codigo.length() != 3) {
            throw new IllegalArgumentException("La moneda debe tener 3 caracteres");
        }
        if (monto.signum() < 0) {
            throw new IllegalArgumentException("El monto no puede ser negativo");
        }
        moneda = codigo;
        monto = monto.setScale(ESCALA, REDONDEO);
    }

    public static Dinero desdeCentavos(long centavos) {
        if (centavos < 0) {
            throw new IllegalArgumentException("Los centavos no pueden ser negativos");
        }
        return new Dinero(BigDecimal.valueOf(centavos).movePointLeft(ESCALA), USD);
    }

    public static Dinero de(BigDecimal monto, String moneda) {
        return new Dinero(monto, moneda);
    }

    public long centavos() {
        return monto.movePointRight(ESCALA).setScale(0, REDONDEO).longValueExact();
    }
}

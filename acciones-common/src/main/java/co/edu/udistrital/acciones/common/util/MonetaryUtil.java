package co.edu.udistrital.acciones.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MonetaryUtil {
    private MonetaryUtil() {}
    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    public static BigDecimal of(String v) { return new BigDecimal(v).setScale(SCALE, ROUNDING); }
    public static BigDecimal of(double v) { return BigDecimal.valueOf(v).setScale(SCALE, ROUNDING); }
    public static BigDecimal of(BigDecimal v) { return v.setScale(SCALE, ROUNDING); }
    public static BigDecimal add(BigDecimal a, BigDecimal b) { return of(a.add(b)); }
    public static BigDecimal sub(BigDecimal a, BigDecimal b) { return of(a.subtract(b)); }
    public static BigDecimal mul(BigDecimal a, BigDecimal b) { return of(a.multiply(b)); }
    public static BigDecimal div(BigDecimal a, BigDecimal b) { return of(a.divide(b, SCALE, ROUNDING)); }
    public static BigDecimal pct(BigDecimal a, BigDecimal p) { return of(a.multiply(p).divide(BigDecimal.valueOf(100), SCALE, ROUNDING)); }
    public static boolean pos(BigDecimal v) { return v.compareTo(BigDecimal.ZERO) > 0; }
    public static boolean gte(BigDecimal a, BigDecimal b) { return a.compareTo(b) >= 0; }
}
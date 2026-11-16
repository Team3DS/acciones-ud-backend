package co.edu.udistrital.acciones.m8notificaciones.domain;

import java.math.BigDecimal;

public enum CondicionPrecio {
    SUPERA,
    CAE_BAJO;

    public boolean cumple(BigDecimal precio, BigDecimal umbral) {
        if (precio == null || umbral == null || precio.signum() <= 0 || umbral.signum() <= 0) {
            return false;
        }
        return this == SUPERA ? precio.compareTo(umbral) > 0 : precio.compareTo(umbral) < 0;
    }
}

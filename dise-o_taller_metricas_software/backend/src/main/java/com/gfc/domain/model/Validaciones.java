package com.gfc.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

final class Validaciones {
    private Validaciones() { }

    static String texto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " es obligatorio");
        }
        return valor.strip();
    }

    static int positivo(int valor, String campo) {
        if (valor <= 0) {
            throw new IllegalArgumentException(campo + " debe ser positivo");
        }
        return valor;
    }

    static int noNegativo(int valor, String campo) {
        if (valor < 0) {
            throw new IllegalArgumentException(campo + " no puede ser negativo");
        }
        return valor;
    }

    static BigDecimal dinero(BigDecimal valor, String campo) {
        Objects.requireNonNull(valor, campo);
        if (valor.signum() < 0) {
            throw new IllegalArgumentException(campo + " no puede ser negativo");
        }
        try {
            BigDecimal pesos = valor.setScale(2, RoundingMode.UNNECESSARY);
            if (pesos.precision() > 19) {
                throw new IllegalArgumentException(campo + " excede la precision de almacenamiento");
            }
            return pesos;
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException(campo + " admite como maximo dos decimales en COP", ex);
        }
    }
}

package co.edu.udistrital.acciones.m6portafolio.domain;

import co.edu.udistrital.acciones.m6portafolio.domain.excepcion.SimboloInvalidoException;

import java.util.Locale;
import java.util.regex.Pattern;

public final class Simbolos {

    public static final String PATRON_TEXTO = "[A-Za-z0-9][A-Za-z0-9.\\-]{0,19}";
    private static final Pattern PATRON = Pattern.compile(PATRON_TEXTO);

    private Simbolos() {
    }

    public static String normalizar(String symbol) {
        if (symbol == null) {
            throw new SimboloInvalidoException("El simbolo es obligatorio");
        }
        String normalizado = symbol.trim().toUpperCase(Locale.ROOT);
        if (!PATRON.matcher(normalizado).matches()) {
            throw new SimboloInvalidoException("El simbolo no es valido: " + symbol);
        }
        return normalizado;
    }
}

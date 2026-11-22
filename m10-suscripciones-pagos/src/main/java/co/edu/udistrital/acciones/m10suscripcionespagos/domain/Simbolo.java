package co.edu.udistrital.acciones.m10suscripcionespagos.domain;

import co.edu.udistrital.acciones.m10suscripcionespagos.domain.excepcion.SimboloInvalidoException;

import java.util.Locale;
import java.util.regex.Pattern;

public record Simbolo(String valor) {

    private static final Pattern PATRON = Pattern.compile("[A-Za-z0-9][A-Za-z0-9.\\-]{0,19}");

    public Simbolo {
        if (valor == null) {
            throw new SimboloInvalidoException("El simbolo es obligatorio");
        }
        String normalizado = valor.trim().toUpperCase(Locale.ROOT);
        if (!PATRON.matcher(normalizado).matches()) {
            throw new SimboloInvalidoException("El simbolo no es valido: " + valor);
        }
        valor = normalizado;
    }

    public static Simbolo desde(String texto) {
        return new Simbolo(texto);
    }
}

package co.edu.udistrital.acciones.m8notificaciones.domain;

public final class DireccionCorreo {
    private DireccionCorreo() {}

    public static boolean valida(String email) {
        if (email == null || email.length() > 254) {
            return false;
        }
        int at = -1;
        for (int index = 0; index < email.length(); index++) {
            char value = email.charAt(index);
            if (Character.isWhitespace(value)
                    || Character.isISOControl(value)
                    || value == '<'
                    || value == '>') {
                return false;
            }
            if (value == '@') {
                if (at >= 0) {
                    return false;
                }
                at = index;
            }
        }
        int dot = email.lastIndexOf('.');
        return at > 0 && dot > at + 1 && dot < email.length() - 1;
    }
}

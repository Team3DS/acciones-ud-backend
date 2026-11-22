package co.edu.udistrital.acciones.m10suscripcionespagos.domain;

public enum EstadoRecibo {
    ABIERTO,
    PAGADO,
    FALLIDO,
    ANULADO,
    INCOBRABLE;

    public static EstadoRecibo desdeStripe(String estado, long intentos) {
        if (estado == null) {
            return ABIERTO;
        }
        return switch (estado.strip().toLowerCase()) {
            case "paid" -> PAGADO;
            case "void" -> ANULADO;
            case "uncollectible" -> INCOBRABLE;
            case "open" -> intentos > 0 ? FALLIDO : ABIERTO;
            default -> ABIERTO;
        };
    }
}

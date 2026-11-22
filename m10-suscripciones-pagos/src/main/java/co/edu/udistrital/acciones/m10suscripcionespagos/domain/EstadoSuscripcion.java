package co.edu.udistrital.acciones.m10suscripcionespagos.domain;

public enum EstadoSuscripcion {
    INCOMPLETA,
    ACTIVA,
    EN_MORA,
    CANCELADA,
    VENCIDA;

    public boolean otorgaPremium() {
        return this == ACTIVA || this == EN_MORA;
    }

    public boolean esTerminal() {
        return this == CANCELADA || this == VENCIDA;
    }

    public boolean esVigente() {
        return this == INCOMPLETA || this == ACTIVA || this == EN_MORA;
    }

    public boolean puedeTransicionarA(EstadoSuscripcion nuevo) {
        if (nuevo == null) {
            return false;
        }
        if (nuevo == this) {
            return true;
        }
        return switch (this) {
            case INCOMPLETA -> nuevo == ACTIVA || nuevo == VENCIDA;
            case ACTIVA -> nuevo == EN_MORA || nuevo == CANCELADA || nuevo == VENCIDA;
            case EN_MORA -> nuevo == ACTIVA || nuevo == CANCELADA || nuevo == VENCIDA;
            case CANCELADA, VENCIDA -> false;
        };
    }
}

package co.edu.udistrital.acciones.m4datosmercado.domain;

public enum Intervalo {
    M1("1m"),
    M5("5m"),
    M15("15m"),
    H1("1h"),
    D1("1d");

    private final String valor;

    Intervalo(String valor) {
        this.valor = valor;
    }

    public String getValor() {
        return valor;
    }

    public static Intervalo fromString(String valor) {
        for (Intervalo intervalo : values()) {
            if (intervalo.valor.equalsIgnoreCase(valor)) {
                return intervalo;
            }
        }
        throw new IllegalArgumentException("Intervalo no válido: " + valor);
    }
}
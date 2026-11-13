package co.edu.udistrital.acciones.m3catalogomercados.domain;

public record Mercado(
        short id,
        String code,
        String name,
        String mic,
        String timeZone,
        String currency,
        boolean enabled,
        String country,
        HorarioReferencia tradingHours) {
    public Mercado(
            short id,
            String code,
            String name,
            String mic,
            String timeZone,
            String currency,
            boolean enabled) {
        this(id, code, name, mic, timeZone, currency, enabled, null, null);
    }
}

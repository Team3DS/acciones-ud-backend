package co.edu.udistrital.acciones.m3catalogomercados.domain;

public record FiltroInstrumentos(
        String query,
        String market,
        String sector,
        Boolean enabled,
        int page,
        int size,
        OrdenCatalogo sort,
        boolean descending,
        String symbol,
        String type) {
    public FiltroInstrumentos(
            String query,
            String market,
            String sector,
            Boolean enabled,
            int page,
            int size,
            OrdenCatalogo sort,
            boolean descending) {
        this(query, market, sector, enabled, page, size, sort, descending, null, null);
    }
}

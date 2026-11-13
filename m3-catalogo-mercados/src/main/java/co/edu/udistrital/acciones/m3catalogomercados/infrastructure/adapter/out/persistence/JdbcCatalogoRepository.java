package co.edu.udistrital.acciones.m3catalogomercados.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m3catalogomercados.application.port.out.CatalogoRepositoryPort;
import co.edu.udistrital.acciones.m3catalogomercados.domain.EstadoInstrumento;
import co.edu.udistrital.acciones.m3catalogomercados.domain.FiltroInstrumentos;
import co.edu.udistrital.acciones.m3catalogomercados.domain.HorarioReferencia;
import co.edu.udistrital.acciones.m3catalogomercados.domain.Instrumento;
import co.edu.udistrital.acciones.m3catalogomercados.domain.Mercado;
import co.edu.udistrital.acciones.m3catalogomercados.domain.Pagina;
import co.edu.udistrital.acciones.m3catalogomercados.domain.Sector;
import co.edu.udistrital.acciones.m3catalogomercados.domain.TipoInstrumento;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Repository
public class JdbcCatalogoRepository implements CatalogoRepositoryPort {
    private static final String MERCADO =
            "m.id AS mercado_id, m.codigo AS mercado_codigo, m.nombre AS mercado_nombre,"
                    + " m.codigo_mic, m.zona_horaria, m.moneda, m.habilitado AS mercado_habilitado,"
                    + " m.pais, m.horario_apertura, m.horario_cierre";
    private static final String SECTOR =
            "s.id AS sector_id, s.codigo AS sector_codigo, s.nombre AS sector_nombre,"
                    + " s.descripcion";
    private static final String COLUMNAS =
            "a.id, a.ticker, a.nombre_empresa, a.habilitada, a.tipo, a.lot_size, a.tick_size,"
                    + " a.estado, a.creado_en, a.actualizado_en, "
                    + MERCADO
                    + ", "
                    + SECTOR;
    private static final String FROM =
            " FROM accion a JOIN mercado m ON m.id = a.mercado_id JOIN sector s ON s.id ="
                    + " a.sector_id";
    private static final RowMapper<Mercado> MERCADO_MAPPER = (rs, row) -> mercado(rs);
    private static final RowMapper<Sector> SECTOR_MAPPER = (rs, row) -> sector(rs);
    private static final RowMapper<Instrumento> INSTRUMENTO_MAPPER =
            (rs, row) ->
                    new Instrumento(
                            rs.getLong("id"),
                            rs.getString("ticker"),
                            rs.getString("nombre_empresa"),
                            rs.getBoolean("habilitada"),
                            mercado(rs),
                            sector(rs),
                            TipoInstrumento.valueOf(rs.getString("tipo")),
                            rs.getBigDecimal("lot_size"),
                            rs.getBigDecimal("tick_size"),
                            EstadoInstrumento.valueOf(rs.getString("estado")),
                            rs.getTimestamp("creado_en").toInstant(),
                            rs.getTimestamp("actualizado_en").toInstant());
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcCatalogoRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Mercado> listarMercados(Boolean enabled) {
        String where = enabled == null ? "" : " WHERE m.habilitado = :enabled";
        return jdbc.query(
                "SELECT " + MERCADO + " FROM mercado m" + where + " ORDER BY m.codigo",
                enabled == null ? Map.of() : Map.of("enabled", enabled),
                MERCADO_MAPPER);
    }

    @Override
    public Optional<Mercado> buscarMercado(String code) {
        return jdbc
                .query(
                        "SELECT "
                                + MERCADO
                                + " FROM mercado m WHERE m.codigo = :code OR m.codigo_mic = :code",
                        Map.of("code", code),
                        MERCADO_MAPPER)
                .stream()
                .findFirst();
    }

    @Override
    public List<Sector> listarSectores() {
        return jdbc.query("SELECT " + SECTOR + " FROM sector s ORDER BY s.codigo", SECTOR_MAPPER);
    }

    @Override
    public Pagina<Instrumento> buscar(FiltroInstrumentos filtro) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        Map<String, Object> params = new HashMap<>();
        if (filtro.type() != null) {
            where.append(" AND a.tipo=:type");
            params.put("type", filtro.type());
        }
        if (filtro.symbol() != null) {
            where.append(" AND a.ticker = :symbol");
            params.put("symbol", filtro.symbol());
        }
        if (filtro.query() != null && !filtro.query().isBlank()) {
            where.append(
                    " AND (a.ticker ILIKE :query ESCAPE '!' OR a.nombre_empresa ILIKE :query ESCAPE"
                            + " '!')");
            params.put(
                    "query",
                    "%"
                            + filtro.query()
                                    .replace("!", "!!")
                                    .replace("%", "!%")
                                    .replace("_", "!_")
                            + "%");
        }
        if (filtro.market() != null) {
            where.append(" AND (m.codigo = :market OR m.codigo_mic = :market)");
            params.put("market", filtro.market());
        }
        if (filtro.sector() != null) {
            where.append(" AND s.codigo = :sector");
            params.put("sector", filtro.sector());
        }
        if (filtro.enabled() != null) {
            where.append(" AND (a.habilitada AND m.habilitado AND a.estado='ACTIVO') = :enabled");
            params.put("enabled", filtro.enabled());
        }
        Long total = jdbc.queryForObject("SELECT COUNT(*)" + FROM + where, params, Long.class);
        String sort =
                switch (filtro.sort()) {
                    case SYMBOL -> "a.ticker";
                    case COMPANY_NAME -> "a.nombre_empresa";
                    case MARKET -> "m.codigo";
                    case SECTOR -> "s.codigo";
                };
        params.put("limit", filtro.size());
        params.put("offset", (long) filtro.page() * filtro.size());
        List<Instrumento> rows =
                jdbc.query(
                        "SELECT "
                                + COLUMNAS
                                + FROM
                                + where
                                + " ORDER BY "
                                + sort
                                + (filtro.descending() ? " DESC" : " ASC")
                                + ", a.id ASC LIMIT :limit OFFSET :offset",
                        params,
                        INSTRUMENTO_MAPPER);
        return Pagina.of(rows, filtro.page(), filtro.size(), Objects.requireNonNull(total));
    }

    @Override
    public List<Instrumento> buscarPorSimbolos(Collection<String> symbols, String market) {
        if (symbols.isEmpty()) return List.of();
        Map<String, Object> params = new HashMap<>();
        params.put("symbols", symbols);
        String where = " WHERE a.ticker IN (:symbols)";
        if (market != null) {
            where += " AND (m.codigo = :market OR m.codigo_mic = :market)";
            params.put("market", market);
        }
        return jdbc.query(
                "SELECT " + COLUMNAS + FROM + where + " ORDER BY a.ticker, m.codigo",
                params,
                INSTRUMENTO_MAPPER);
    }

    @Override
    public Optional<Instrumento> buscarPorId(long id) {
        return jdbc
                .query(
                        "SELECT " + COLUMNAS + FROM + " WHERE a.id = :id",
                        Map.of("id", id),
                        INSTRUMENTO_MAPPER)
                .stream()
                .findFirst();
    }

    private static Mercado mercado(ResultSet rs) throws SQLException {
        return new Mercado(
                rs.getShort("mercado_id"),
                rs.getString("mercado_codigo"),
                rs.getString("mercado_nombre"),
                rs.getString("codigo_mic").strip(),
                rs.getString("zona_horaria"),
                rs.getString("moneda").strip(),
                rs.getBoolean("mercado_habilitado"),
                rs.getString("pais"),
                rs.getTime("horario_apertura") == null
                        ? null
                        : new HorarioReferencia(
                                rs.getTime("horario_apertura").toLocalTime(),
                                rs.getTime("horario_cierre").toLocalTime()));
    }

    private static Sector sector(ResultSet rs) throws SQLException {
        return new Sector(
                rs.getShort("sector_id"),
                rs.getString("sector_codigo"),
                rs.getString("sector_nombre"),
                rs.getString("descripcion"));
    }
}

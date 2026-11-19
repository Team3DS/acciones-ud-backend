package co.edu.udistrital.acciones.m3catalogomercados.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m3catalogomercados.application.command.ActualizarInstrumentoCommand;
import co.edu.udistrital.acciones.m3catalogomercados.application.command.ActualizarMercadoCommand;
import co.edu.udistrital.acciones.m3catalogomercados.application.command.CrearInstrumentoCommand;
import co.edu.udistrital.acciones.m3catalogomercados.application.command.CrearMercadoCommand;
import co.edu.udistrital.acciones.m3catalogomercados.application.port.out.CatalogoAdminPort;
import co.edu.udistrital.acciones.m3catalogomercados.application.port.out.CatalogoRepositoryPort;
import co.edu.udistrital.acciones.m3catalogomercados.domain.CatalogoException;
import co.edu.udistrital.acciones.m3catalogomercados.domain.EstadoInstrumento;
import co.edu.udistrital.acciones.m3catalogomercados.domain.EventoCatalogo;
import co.edu.udistrital.acciones.m3catalogomercados.domain.HorarioReferencia;
import co.edu.udistrital.acciones.m3catalogomercados.domain.Instrumento;
import co.edu.udistrital.acciones.m3catalogomercados.domain.Mercado;
import co.edu.udistrital.acciones.m3catalogomercados.domain.ResultadoComando;
import co.edu.udistrital.acciones.m3catalogomercados.domain.Sector;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.MDC;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

@Repository
public class JdbcCatalogoAdminRepository implements CatalogoAdminPort {
    private final NamedParameterJdbcTemplate jdbc;
    private final CatalogoRepositoryPort catalogo;
    private final ObjectMapper mapper;

    public JdbcCatalogoAdminRepository(
            NamedParameterJdbcTemplate jdbc, CatalogoRepositoryPort catalogo, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.catalogo = catalogo;
        this.mapper = mapper;
    }

    @Override
    public <T> ResultadoComando<T> ejecutar(
            String operation,
            String key,
            long actor,
            Object request,
            Class<T> type,
            Supplier<ResultadoComando<T>> action) {
        if (key == null || !key.matches("[A-Za-z0-9._-]{1,100}"))
            throw new IllegalArgumentException(
                    "Idempotency-Key requiere de 1 a 100 letras, números, puntos, guiones o guiones"
                            + " bajos.");
        String hash = hash(json(request));
        var params =
                new MapSqlParameterSource()
                        .addValue("operation", operation)
                        .addValue("key", key)
                        .addValue("actor", actor)
                        .addValue("hash", hash);
        int inserted =
                jdbc.update(
                        "INSERT INTO"
                            + " comando_catalogo(actor_id,operacion,idempotency_key,fingerprint)"
                            + " VALUES(:actor,:operation,:key,:hash) ON CONFLICT DO NOTHING",
                        params);
        Map<String, Object> stored =
                jdbc.queryForMap(
                        "SELECT fingerprint,status,body::text AS body,location FROM"
                            + " comando_catalogo WHERE actor_id=:actor AND operacion=:operation AND"
                            + " idempotency_key=:key FOR UPDATE",
                        params);
        if (!hash.equals(stored.get("fingerprint")))
            throw new CatalogoException(
                    "IDEMPOTENCIA_CONFLICTO", 409, "La clave ya se usó con otro contenido.");
        if (inserted == 0) {
            if (stored.get("status") == null)
                throw new CatalogoException(
                        "COMANDO_PENDIENTE", 409, "El comando todavía está en proceso.");
            try {
                return new ResultadoComando<>(
                        ((Number) stored.get("status")).intValue(),
                        (String) stored.get("location"),
                        mapper.readValue((String) stored.get("body"), type),
                        true);
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException(
                        "No se pudo recuperar el resultado del comando.", exception);
            }
        }
        ResultadoComando<T> result = action.get();
        params.addValue("status", result.status())
                .addValue("body", json(result.body()))
                .addValue("location", result.location());
        jdbc.update(
                "UPDATE comando_catalogo SET status=:status,body=CAST(:body AS"
                    + " jsonb),location=:location WHERE actor_id=:actor AND operacion=:operation"
                    + " AND idempotency_key=:key",
                params);
        return result;
    }

    @Override
    public Mercado crearMercado(CrearMercadoCommand command) {
        String code = command.code() == null ? upper(command.mic()) : upper(command.code());
        if (!code.matches("[A-Z0-9_]{1,30}"))
            throw new IllegalArgumentException("Código de mercado inválido.");
        var params =
                new MapSqlParameterSource()
                        .addValue("code", code)
                        .addValue("name", command.name().strip())
                        .addValue("mic", upper(command.mic()))
                        .addValue("zone", command.timeZone())
                        .addValue(
                                "currency", command.currency() == null ? "USD" : command.currency())
                        .addValue("country", command.country())
                        .addValue(
                                "open",
                                command.tradingHours() == null
                                        ? null
                                        : command.tradingHours().open())
                        .addValue(
                                "close",
                                command.tradingHours() == null
                                        ? null
                                        : command.tradingHours().close());
        jdbc.update(
                "INSERT INTO"
                    + " mercado(codigo,nombre,codigo_mic,zona_horaria,moneda,pais,horario_apertura,horario_cierre)"
                    + " VALUES(:code,:name,:mic,:zone,:currency,:country,:open,:close)",
                params);
        return mercado(code);
    }

    @Override
    public Mercado actualizarMercado(String code, ActualizarMercadoCommand command) {
        Mercado current = mercado(code);
        jdbc.queryForObject(
                "SELECT id FROM mercado WHERE id=:id FOR UPDATE",
                Map.of("id", current.id()),
                Short.class);
        current = mercado(code);
        HorarioReferencia hours =
                command.tradingHours() == null ? current.tradingHours() : command.tradingHours();
        var params =
                new MapSqlParameterSource()
                        .addValue("id", current.id())
                        .addValue(
                                "name",
                                command.name() == null ? current.name() : command.name().strip())
                        .addValue(
                                "enabled",
                                command.enabled() == null ? current.enabled() : command.enabled())
                        .addValue(
                                "country",
                                command.country() == null ? current.country() : command.country())
                        .addValue("open", hours == null ? null : hours.open())
                        .addValue("close", hours == null ? null : hours.close());
        jdbc.update(
                "UPDATE mercado SET"
                    + " nombre=:name,habilitado=:enabled,pais=:country,horario_apertura=:open,horario_cierre=:close"
                    + " WHERE id=:id",
                params);
        return mercado(code);
    }

    @Override
    public Instrumento crearInstrumento(CrearInstrumentoCommand command) {
        Mercado market = mercado(command.market());
        Sector sector =
                catalogo.listarSectores().stream()
                        .filter(s -> s.code().equals(upper(command.sector())))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new CatalogoException(
                                                "SECTOR_NO_ENCONTRADO",
                                                404,
                                                "El sector no existe."));
        var params =
                new MapSqlParameterSource()
                        .addValue("market", market.id())
                        .addValue("sector", sector.id())
                        .addValue("symbol", upper(command.symbol()))
                        .addValue("name", command.name().strip())
                        .addValue("type", command.type().name())
                        .addValue("lot", command.lotSize())
                        .addValue("tick", command.tickSize());
        Long id =
                jdbc.queryForObject(
                        "INSERT INTO"
                            + " accion(mercado_id,sector_id,ticker,nombre_empresa,tipo,lot_size,tick_size)"
                            + " VALUES(:market,:sector,:symbol,:name,:type,:lot,:tick) RETURNING"
                            + " id",
                        params,
                        Long.class);
        return catalogo.buscarPorId(Objects.requireNonNull(id)).orElseThrow();
    }

    @Override
    public Instrumento actualizarInstrumento(
            String symbol, String market, ActualizarInstrumentoCommand command) {
        List<Instrumento> matches =
                catalogo.buscarPorSimbolos(
                        List.of(upper(symbol)), market == null ? null : upper(market));
        if (matches.isEmpty())
            throw new CatalogoException(
                    "INSTRUMENTO_NO_ENCONTRADO", 404, "El instrumento no existe.");
        if (matches.size() > 1)
            throw new CatalogoException(
                    "SIMBOLO_AMBIGUO", 409, "Indique el mercado del instrumento.");
        long id = matches.getFirst().id();
        jdbc.queryForObject(
                "SELECT id FROM accion WHERE id=:id FOR UPDATE", Map.of("id", id), Long.class);
        Instrumento current = catalogo.buscarPorId(id).orElseThrow();
        EstadoInstrumento state = command.state() == null ? current.state() : command.state();
        var params =
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue(
                                "name",
                                command.name() == null
                                        ? current.companyName()
                                        : command.name().strip())
                        .addValue(
                                "lot",
                                command.lotSize() == null ? current.lotSize() : command.lotSize())
                        .addValue(
                                "tick",
                                command.tickSize() == null
                                        ? current.tickSize()
                                        : command.tickSize())
                        .addValue("state", state.name())
                        .addValue("enabled", state == EstadoInstrumento.ACTIVO);
        jdbc.update(
                "UPDATE accion SET"
                    + " nombre_empresa=:name,lot_size=:lot,tick_size=:tick,estado=:state,habilitada=:enabled,actualizado_en=CURRENT_TIMESTAMP"
                    + " WHERE id=:id",
                params);
        return catalogo.buscarPorId(id).orElseThrow();
    }

    @Override
    public void registrarEvento(String routingKey, String aggregate, Object payload) {
        UUID id = UUID.randomUUID();
        EventoCatalogo event =
                new EventoCatalogo(
                        id,
                        routingKey,
                        Instant.now(),
                        aggregate,
                        MDC.get("correlationId"),
                        payload);
        jdbc.update(
                "INSERT INTO outbox_evento(id,routing_key,body) VALUES(:id,:routing,CAST(:body AS"
                        + " jsonb))",
                Map.of("id", id, "routing", routingKey, "body", json(event)));
    }

    private Mercado mercado(String code) {
        return catalogo.buscarMercado(upper(code))
                .orElseThrow(
                        () ->
                                new CatalogoException(
                                        "MERCADO_NO_ENCONTRADO", 404, "El mercado no existe."));
    }

    private String json(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudo serializar el catálogo.", exception);
        }
    }

    private static String upper(String value) {
        return value.strip().toUpperCase(Locale.ROOT);
    }

    private static String hash(String value) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}

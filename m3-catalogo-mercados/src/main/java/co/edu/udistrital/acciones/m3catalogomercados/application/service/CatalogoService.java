package co.edu.udistrital.acciones.m3catalogomercados.application.service;

import co.edu.udistrital.acciones.m3catalogomercados.application.port.out.CatalogoRepositoryPort;
import co.edu.udistrital.acciones.m3catalogomercados.domain.CatalogoException;
import co.edu.udistrital.acciones.m3catalogomercados.domain.FiltroInstrumentos;
import co.edu.udistrital.acciones.m3catalogomercados.domain.Instrumento;
import co.edu.udistrital.acciones.m3catalogomercados.domain.Mercado;
import co.edu.udistrital.acciones.m3catalogomercados.domain.MotivoElegibilidad;
import co.edu.udistrital.acciones.m3catalogomercados.domain.OrdenCatalogo;
import co.edu.udistrital.acciones.m3catalogomercados.domain.Pagina;
import co.edu.udistrital.acciones.m3catalogomercados.domain.Sector;
import co.edu.udistrital.acciones.m3catalogomercados.domain.ValidacionInstrumento;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CatalogoService {
    private static final Pattern SYMBOL = Pattern.compile("[A-Z0-9][A-Z0-9.\\-]{0,19}");
    private static final Pattern CODE = Pattern.compile("[A-Z0-9_]{1,30}");
    private final CatalogoRepositoryPort repository;

    public CatalogoService(CatalogoRepositoryPort repository) {
        this.repository = repository;
    }

    public List<Mercado> mercados(Boolean enabled) {
        return repository.listarMercados(enabled);
    }

    public List<Sector> sectores() {
        return repository.listarSectores();
    }

    public Mercado mercado(String code) {
        return repository
                .buscarMercado(codigo(code))
                .orElseThrow(
                        () ->
                                new CatalogoException(
                                        "MERCADO_NO_ENCONTRADO", 404, "El mercado no existe."));
    }

    public Instrumento instrumento(long id) {
        if (id <= 0) throw new IllegalArgumentException("El identificador debe ser positivo.");
        return repository
                .buscarPorId(id)
                .orElseThrow(
                        () ->
                                new CatalogoException(
                                        "INSTRUMENTO_NO_ENCONTRADO",
                                        404,
                                        "El instrumento no existe."));
    }

    public Instrumento instrumento(String symbol, String market) {
        List<Instrumento> encontrados =
                repository.buscarPorSimbolos(List.of(simbolo(symbol)), codigoOpcional(market));
        if (encontrados.isEmpty())
            throw new CatalogoException(
                    "INSTRUMENTO_NO_ENCONTRADO", 404, "El instrumento no existe.");
        if (encontrados.size() > 1)
            throw new CatalogoException(
                    "SIMBOLO_AMBIGUO", 409, "Indique el mercado del instrumento.");
        return encontrados.getFirst();
    }

    public Pagina<Instrumento> buscar(
            String query,
            String market,
            String sector,
            Boolean enabled,
            int page,
            int size,
            String sort,
            String direction) {
        return buscar(query, market, sector, enabled, page, size, sort, direction, null, null);
    }

    public Pagina<Instrumento> buscar(
            String query,
            String market,
            String sector,
            Boolean enabled,
            int page,
            int size,
            String sort,
            String direction,
            String symbol,
            String type) {
        if (page < 0 || size < 1 || size > 100)
            throw new IllegalArgumentException(
                    "La página debe ser positiva o cero y el tamaño debe estar entre 1 y 100.");
        String text = query == null ? null : query.strip();
        if (text != null && text.length() > 150)
            throw new IllegalArgumentException("La búsqueda admite hasta 150 caracteres.");
        OrdenCatalogo order;
        try {
            order = OrdenCatalogo.valueOf(sort.toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(
                    "Orden permitido: SYMBOL, COMPANY_NAME, MARKET o SECTOR. Los precios pertenecen"
                            + " a M4.");
        }
        if (!"asc".equalsIgnoreCase(direction) && !"desc".equalsIgnoreCase(direction))
            throw new IllegalArgumentException("La dirección debe ser asc o desc.");
        return repository.buscar(
                new FiltroInstrumentos(
                        text,
                        codigoOpcional(market),
                        codigoOpcional(sector),
                        enabled,
                        page,
                        size,
                        order,
                        "desc".equalsIgnoreCase(direction),
                        symbol == null ? null : simbolo(symbol),
                        tipo(type)));
    }

    public List<ValidacionInstrumento> validar(List<String> symbols, String market) {
        if (symbols == null || symbols.isEmpty() || symbols.size() > 100)
            throw new IllegalArgumentException("Envíe entre 1 y 100 símbolos.");
        List<String> normalizados =
                symbols.stream().map(CatalogoService::simbolo).distinct().toList();
        Map<String, List<Instrumento>> porSimbolo =
                repository.buscarPorSimbolos(normalizados, codigoOpcional(market)).stream()
                        .collect(Collectors.groupingBy(Instrumento::symbol));
        return normalizados.stream()
                .map(symbol -> evaluar(symbol, porSimbolo.getOrDefault(symbol, List.of())))
                .toList();
    }

    private static ValidacionInstrumento evaluar(String symbol, List<Instrumento> encontrados) {
        if (encontrados.isEmpty())
            return new ValidacionInstrumento(symbol, false, MotivoElegibilidad.NO_ENCONTRADO, null);
        if (encontrados.size() > 1)
            return new ValidacionInstrumento(
                    symbol, false, MotivoElegibilidad.SIMBOLO_AMBIGUO, null);
        Instrumento instrumento = encontrados.getFirst();
        MotivoElegibilidad motivo =
                !instrumento.market().enabled()
                        ? MotivoElegibilidad.MERCADO_DESHABILITADO
                        : !instrumento.enabled()
                                        || instrumento.state()
                                                != co.edu.udistrital.acciones.m3catalogomercados
                                                        .domain.EstadoInstrumento.ACTIVO
                                ? MotivoElegibilidad.ACCION_DESHABILITADA
                                : MotivoElegibilidad.HABILITADO;
        return new ValidacionInstrumento(symbol, instrumento.tradable(), motivo, instrumento);
    }

    private static String tipo(String type) {
        if (type == null) return null;
        try {
            return co.edu.udistrital.acciones.m3catalogomercados.domain.TipoInstrumento.valueOf(
                            type.toUpperCase(Locale.ROOT))
                    .name();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Tipo de instrumento desconocido.");
        }
    }

    private static String simbolo(String value) {
        String result = normalizar(value);
        if (!SYMBOL.matcher(result).matches())
            throw new IllegalArgumentException(
                    "Símbolo inválido: use de 1 a 20 letras, números, puntos o guiones.");
        return result;
    }

    private static String codigo(String value) {
        String result = normalizar(value);
        if (!CODE.matcher(result).matches())
            throw new IllegalArgumentException(
                    "Código inválido: use hasta 30 letras, números o guiones bajos.");
        return result;
    }

    private static String codigoOpcional(String value) {
        return value == null ? null : codigo(value);
    }

    private static String normalizar(String value) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("El valor no puede estar vacío.");
        return value.strip().toUpperCase(Locale.ROOT);
    }
}

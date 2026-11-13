package co.edu.udistrital.acciones.m3catalogomercados.infrastructure.adapter.in.rest;

import co.edu.udistrital.acciones.m3catalogomercados.application.service.CatalogoService;
import co.edu.udistrital.acciones.m3catalogomercados.domain.Instrumento;
import co.edu.udistrital.acciones.m3catalogomercados.domain.Pagina;
import co.edu.udistrital.acciones.m3catalogomercados.domain.Sector;
import co.edu.udistrital.acciones.m3catalogomercados.domain.ValidacionInstrumento;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/instruments")
public class InstrumentoController {
    private final CatalogoService catalogo;

    public InstrumentoController(CatalogoService catalogo) {
        this.catalogo = catalogo;
    }

    @GetMapping
    public Pagina<Instrumento> listar(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String market,
            @RequestParam(required = false) String sector,
            @RequestParam(defaultValue = "true") Boolean enabled,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "SYMBOL") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam Map<String, String> parameters) {
        query = compatible(query, parameters.get("filter[query]"));
        market = compatible(market, parameters.get("filter[market]"));
        sector = compatible(sector, parameters.get("filter[sector]"));
        String symbol = compatible(parameters.get("symbol"), parameters.get("filter[symbol]"));
        String type = compatible(parameters.get("type"), parameters.get("filter[type]"));
        String filterEnabled = parameters.get("filter[enabled]");
        if (filterEnabled != null) {
            String value = compatible(parameters.get("enabled"), filterEnabled);
            if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value))
                throw new IllegalArgumentException("enabled debe ser true o false.");
            enabled = Boolean.valueOf(value);
        }
        String[] order = sort.split(",", -1);
        if (order.length > 2)
            throw new IllegalArgumentException("Use sort=campo,asc o sort=campo,desc.");
        if (order.length == 2) direction = compatible(parameters.get("direction"), order[1]);
        sort = "companyName".equalsIgnoreCase(order[0]) ? "COMPANY_NAME" : order[0];
        return catalogo.buscar(
                query, market, sector, enabled, page, size, sort, direction, symbol, type);
    }

    @GetMapping("/sectors")
    public List<Sector> sectores() {
        return catalogo.sectores();
    }

    @GetMapping("/id/{id}")
    public Instrumento porId(@PathVariable long id) {
        return catalogo.instrumento(id);
    }

    @GetMapping("/{symbol}")
    public Instrumento detalle(
            @PathVariable String symbol, @RequestParam(required = false) String market) {
        return catalogo.instrumento(symbol, market);
    }

    @GetMapping({"/{symbol}/validation", "/{symbol}/tradable"})
    public ValidacionInstrumento validar(
            @PathVariable String symbol, @RequestParam(required = false) String market) {
        return catalogo.validar(List.of(symbol), market).getFirst();
    }

    @PostMapping("/validate")
    public List<ValidacionInstrumento> validarLote(
            @Valid @RequestBody ValidarInstrumentosRequest request) {
        return catalogo.validar(request.symbols(), request.market());
    }

    private static String compatible(String direct, String alias) {
        if (direct != null && alias != null && !direct.equalsIgnoreCase(alias))
            throw new IllegalArgumentException("Los filtros repetidos deben tener el mismo valor.");
        return direct != null ? direct : alias;
    }
}

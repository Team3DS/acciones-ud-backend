package co.edu.udistrital.acciones.m4datosmercado.infrastructure.adapter.in;

import co.edu.udistrital.acciones.m4datosmercado.application.usecase.ObtenerCotizacionUseCase;
import co.edu.udistrital.acciones.m4datosmercado.application.usecase.ObtenerHistoricoUseCase;
import co.edu.udistrital.acciones.m4datosmercado.domain.Cotizacion;
import co.edu.udistrital.acciones.m4datosmercado.domain.HistoricoCotizacion;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/quotes")
public class CotizacionController {

    private final ObtenerCotizacionUseCase useCase;
    private final ObtenerHistoricoUseCase historicoUseCase;

    public CotizacionController(ObtenerCotizacionUseCase useCase, ObtenerHistoricoUseCase historicoUseCase) {
        this.useCase = useCase;
        this.historicoUseCase = historicoUseCase;
    }

    @GetMapping("/{symbol}")
    public Cotizacion get(@PathVariable String symbol) {
        return useCase.execute(symbol);
    }

    @GetMapping("/latest")
    public List<Cotizacion> latest(@RequestParam List<String> symbols) {
        return useCase.executeMultiple(symbols);
    }

    @GetMapping("/{symbol}/history")
    public List<HistoricoCotizacion> history(
            @PathVariable String symbol,
            @RequestParam Instant from,
            @RequestParam Instant to,
            @RequestParam(defaultValue = "D1") String intervalo) {
        return historicoUseCase.execute(symbol, from, to, co.edu.udistrital.acciones.m4datosmercado.domain.Intervalo.valueOf(intervalo.toUpperCase()));
    }
}

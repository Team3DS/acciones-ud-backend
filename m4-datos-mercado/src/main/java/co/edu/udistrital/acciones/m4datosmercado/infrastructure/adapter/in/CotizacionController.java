package co.edu.udistrital.acciones.m4datosmercado.infrastructure.adapter.in;

import co.edu.udistrital.acciones.m4datosmercado.application.usecase.ObtenerCotizacionUseCase;
import co.edu.udistrital.acciones.m4datosmercado.domain.Cotizacion;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/quotes")
public class CotizacionController {
    private final ObtenerCotizacionUseCase useCase;

    public CotizacionController(ObtenerCotizacionUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/{symbol}")
    public Cotizacion get(@PathVariable String symbol) {
        return useCase.execute(symbol);
    }

    @GetMapping("/latest")
    public List<Cotizacion> latest(@RequestParam List<String> symbols) {
        return useCase.executeMultiple(symbols);
    }
}

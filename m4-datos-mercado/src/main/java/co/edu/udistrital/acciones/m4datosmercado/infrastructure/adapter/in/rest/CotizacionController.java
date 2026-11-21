package co.edu.udistrital.acciones.m4datosmercado.infrastructure.adapter.in.rest;

import co.edu.udistrital.acciones.m4datosmercado.application.dto.CotizacionResponse;
import co.edu.udistrital.acciones.m4datosmercado.application.dto.HistoricoCotizacionResponse;
import co.edu.udistrital.acciones.m4datosmercado.application.port.in.ObtenerCotizacionUseCase;
import co.edu.udistrital.acciones.m4datosmercado.application.port.in.ObtenerHistoricoUseCase;
import co.edu.udistrital.acciones.m4datosmercado.domain.Intervalo;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/quotes")
public class CotizacionController {

    private final ObtenerCotizacionUseCase cotizacionUseCase;
    private final ObtenerHistoricoUseCase historicoUseCase;

    public CotizacionController(ObtenerCotizacionUseCase cotizacionUseCase, ObtenerHistoricoUseCase historicoUseCase) {
        this.cotizacionUseCase = cotizacionUseCase;
        this.historicoUseCase = historicoUseCase;
    }

    @GetMapping("/latest")
    public ResponseEntity<List<CotizacionResponse>> obtenerCotizaciones(@RequestParam String symbols) {
        List<String> symbolList = List.of(symbols.split(","));
        List<CotizacionResponse> response = cotizacionUseCase.obtenerCotizaciones(symbolList).stream()
                .map(CotizacionResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{symbol}")
    public ResponseEntity<CotizacionResponse> obtenerCotizacion(@PathVariable String symbol) {
        CotizacionResponse response = CotizacionResponse.from(cotizacionUseCase.obtenerCotizacion(symbol));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{symbol}/history")
    public ResponseEntity<List<HistoricoCotizacionResponse>> obtenerHistorico(
            @PathVariable String symbol,
            @RequestParam Instant from,
            @RequestParam Instant to,
            @RequestParam(defaultValue = "1d") String interval) {

        Intervalo intervalo = Intervalo.fromString(interval);
        List<HistoricoCotizacionResponse> response = historicoUseCase.obtenerHistorico(symbol, from, to, intervalo).stream()
                .map(HistoricoCotizacionResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }
}
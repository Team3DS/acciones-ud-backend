package co.edu.udistrital.acciones.m4datosmercado.infrastructure.adapter.in.rest;

import co.edu.udistrital.acciones.m4datosmercado.application.dto.EstadoMercadoResponse;
import co.edu.udistrital.acciones.m4datosmercado.application.dto.HorarioMercadoResponse;
import co.edu.udistrital.acciones.m4datosmercado.application.port.in.ConsultarHorariosUseCase;
import co.edu.udistrital.acciones.m4datosmercado.domain.EstadoMercado;
import co.edu.udistrital.acciones.m4datosmercado.domain.HorarioMercado;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/market")
public class MercadoDatosController {

    private final ConsultarHorariosUseCase horariosUseCase;

    public MercadoDatosController(ConsultarHorariosUseCase horariosUseCase) {
        this.horariosUseCase = horariosUseCase;
    }

    @GetMapping("/hours")
    public ResponseEntity<List<HorarioMercadoResponse>> obtenerHorarios(@RequestParam String market) {
        List<HorarioMercadoResponse> response = horariosUseCase.obtenerHorarios(market).stream()
                .map(HorarioMercadoResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<EstadoMercadoResponse> obtenerEstado(@RequestParam String market) {
        Instant now = Instant.now();
        EstadoMercado estado = horariosUseCase.obtenerEstado(market, now);
        EstadoMercadoResponse response = EstadoMercadoResponse.from(market, estado, now);
        return ResponseEntity.ok(response);
    }
}
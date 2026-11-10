package co.edu.udistrital.acciones.m4datosmercado.infrastructure.adapter.in;

import co.edu.udistrital.acciones.m4datosmercado.application.usecase.ConsultarHorariosUseCase;
import co.edu.udistrital.acciones.m4datosmercado.domain.EstadoMercado;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/market")
public class MercadoDatosController {
    private final ConsultarHorariosUseCase useCase;

    public MercadoDatosController(ConsultarHorariosUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/hours")
    public EstadoMercado hours(@RequestParam String market) {
        return useCase.estado(market, Instant.now());
    }

    @GetMapping("/status")
    public String status() {
        return "OK";
    }
}

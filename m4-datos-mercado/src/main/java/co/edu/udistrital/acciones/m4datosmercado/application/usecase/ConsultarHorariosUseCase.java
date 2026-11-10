package co.edu.udistrital.acciones.m4datosmercado.application.usecase;

import co.edu.udistrital.acciones.m4datosmercado.domain.EstadoMercado;
import co.edu.udistrital.acciones.m4datosmercado.domain.HorarioMercado;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ConsultarHorariosUseCase {
    private final List<HorarioMercado> horarios;

    public ConsultarHorariosUseCase() {
        this.horarios = List.of();
    }

    public EstadoMercado estado(String mercadoMic, Instant instant) {
        return horarios.stream()
                .filter(h -> h.getMercadoMic().equalsIgnoreCase(mercadoMic))
                .findFirst()
                .map(h -> h.obtenerEstado(instant))
                .orElse(EstadoMercado.CERRADO);
    }
}

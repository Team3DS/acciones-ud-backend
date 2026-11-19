package co.edu.udistrital.acciones.m4datosmercado.application.usecase;

import co.edu.udistrital.acciones.m4datosmercado.application.port.in.ConsultarHorariosUseCase;
import co.edu.udistrital.acciones.m4datosmercado.application.port.out.CatalogoClientPort;
import co.edu.udistrital.acciones.m4datosmercado.domain.EstadoMercado;
import co.edu.udistrital.acciones.m4datosmercado.domain.HorarioMercado;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ConsultarHorariosUseCaseImpl implements ConsultarHorariosUseCase {

    private final CatalogoClientPort catalogoPort;

    public ConsultarHorariosUseCaseImpl(CatalogoClientPort catalogoPort) {
        this.catalogoPort = catalogoPort;
    }

    @Override
    public List<HorarioMercado> obtenerHorarios(String mercadoMic) {
        return catalogoPort.obtenerHorariosMercado(mercadoMic);
    }

    @Override
    public EstadoMercado obtenerEstado(String mercadoMic, Instant instant) {
        return catalogoPort.obtenerEstadoMercado(mercadoMic, instant);
    }
}
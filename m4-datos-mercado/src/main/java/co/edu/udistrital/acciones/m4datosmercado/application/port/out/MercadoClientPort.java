package co.edu.udistrital.acciones.m4datosmercado.application.port.out;

import co.edu.udistrital.acciones.m4datosmercado.domain.HorarioMercado;
import java.util.List;

public interface MercadoClientPort {
    List<HorarioMercado> obtenerHorarios();
}

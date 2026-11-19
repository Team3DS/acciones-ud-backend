package co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.CrearOrdenUseCase.OrdenResultado;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.Orden;

import java.util.List;

public interface ConsultarOrdenUseCase {
    Orden buscarPorId(Long id);
    List<Orden> buscarPorUsuario(Long usuarioId);
    List<Orden> buscarPorUsuarioYEstado(Long usuarioId, String estado);
    OrdenResultado resumen(Long ordenId);
}

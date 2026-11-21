package co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out;

import co.edu.udistrital.acciones.m5ordenesejecucion.domain.ConfirmacionEjecucion;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.ServicioParticipante;

import java.util.List;
import java.util.Optional;

public interface ConfirmacionEjecucionRepositoryPort {
    Optional<ConfirmacionEjecucion> findByEjecucionIdAndServicio(Long ejecucionOrdenId, ServicioParticipante servicio);
    List<ConfirmacionEjecucion> findByEjecucionId(Long ejecucionOrdenId);
    ConfirmacionEjecucion save(ConfirmacionEjecucion confirmacion);
}

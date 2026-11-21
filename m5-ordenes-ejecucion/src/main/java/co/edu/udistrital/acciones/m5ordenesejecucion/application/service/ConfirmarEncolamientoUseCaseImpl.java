package co.edu.udistrital.acciones.m5ordenesejecucion.application.service;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.ConfirmarEncolamientoUseCase;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.HistorialEstadoOrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.OrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EstadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.HistorialEstadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.Orden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.TipoActor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
public class ConfirmarEncolamientoUseCaseImpl implements ConfirmarEncolamientoUseCase {

    private final OrdenRepositoryPort ordenRepository;
    private final HistorialEstadoOrdenRepositoryPort historialRepository;

    public ConfirmarEncolamientoUseCaseImpl(OrdenRepositoryPort ordenRepository,
                                             HistorialEstadoOrdenRepositoryPort historialRepository) {
        this.ordenRepository = ordenRepository;
        this.historialRepository = historialRepository;
    }

    @Override
    public Orden ejecutar(Long ordenId, Long usuarioId) {
        Orden orden = ordenRepository.findById(ordenId)
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada: " + ordenId));

        if (!orden.getUsuarioId().equals(usuarioId)) {
            throw new IllegalArgumentException("No tiene permisos para confirmar el encolamiento de esta orden");
        }

        if (!Boolean.TRUE.equals(orden.getRequiereEncolamiento())) {
            throw new IllegalStateException("La orden no requiere confirmación de encolamiento");
        }

        if (orden.getEstadoOrden() != EstadoOrden.PENDIENTE) {
            throw new IllegalStateException("Solo se puede confirmar encolamiento en estado PENDIENTE");
        }

        orden.setConfirmadoEncolamientoEn(Instant.now());
        orden.setActualizadaEn(Instant.now());

        historialRepository.save(HistorialEstadoOrden.registrar(
                orden.getId(), EstadoOrden.PENDIENTE, TipoActor.USUARIO, usuarioId,
                "Usuario confirmó explícitamente el encolamiento para la siguiente apertura"));

        return ordenRepository.save(orden);
    }
}

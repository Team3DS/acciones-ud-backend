package co.edu.udistrital.acciones.m5ordenesejecucion.application.service;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.CancelarOrdenUseCase;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.OrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.PublicadorOrdenesPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EstadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.Orden;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
public class CancelarOrdenUseCaseImpl implements CancelarOrdenUseCase {

    private final OrdenRepositoryPort ordenRepository;
    private final PublicadorOrdenesPort publicador;

    public CancelarOrdenUseCaseImpl(OrdenRepositoryPort ordenRepository,
                                    PublicadorOrdenesPort publicador) {
        this.ordenRepository = ordenRepository;
        this.publicador = publicador;
    }

    @Override
    public void ejecutar(Long ordenId, String motivo) {
        Orden orden = ordenRepository.findById(ordenId)
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada: " + ordenId));

        if (!orden.esCancelable()) {
            throw new IllegalArgumentException("Orden no cancelable en estado: " + orden.getEstado());
        }

        orden.setEstado(EstadoOrden.CANCELADA);
        orden.setActualizadoEn(Instant.now());
        ordenRepository.save(orden);

        publicador.publicarOrdenCancelada(ordenId, motivo);
    }
}

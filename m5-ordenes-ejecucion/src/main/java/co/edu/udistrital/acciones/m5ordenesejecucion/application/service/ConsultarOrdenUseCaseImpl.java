package co.edu.udistrital.acciones.m5ordenesejecucion.application.service;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.ConsultarOrdenUseCase;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.CrearOrdenUseCase.OrdenResultado;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.EjecucionRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.OrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EstadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.Orden;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ConsultarOrdenUseCaseImpl implements ConsultarOrdenUseCase {

    private final OrdenRepositoryPort ordenRepository;
    private final EjecucionRepositoryPort ejecucionRepository;

    public ConsultarOrdenUseCaseImpl(OrdenRepositoryPort ordenRepository,
                                     EjecucionRepositoryPort ejecucionRepository) {
        this.ordenRepository = ordenRepository;
        this.ejecucionRepository = ejecucionRepository;
    }

    @Override
    public Orden buscarPorId(Long id) {
        return ordenRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada: " + id));
    }

    @Override
    public List<Orden> buscarPorUsuario(Long usuarioId) {
        return ordenRepository.findByUsuarioId(usuarioId);
    }

    @Override
    public List<Orden> buscarPorUsuarioYEstado(Long usuarioId, String estado) {
        EstadoOrden estadoOrden = EstadoOrden.valueOf(estado);
        return ordenRepository.findByUsuarioIdAndEstado(usuarioId, estadoOrden);
    }

    @Override
    public OrdenResultado resumen(Long ordenId) {
        Orden orden = buscarPorId(ordenId);
        return new OrdenResultado(
                orden.getId(),
                orden.getSimbolo(),
                orden.getLado().name(),
                orden.getTipo().name(),
                orden.getCantidad().intValue(),
                orden.getPrecioLimite(),
                orden.getComision(),
                orden.getValorTotal(),
                orden.getEstado().name(),
                orden.getClientOrderId()
        );
    }
}

package co.edu.udistrital.acciones.m5ordenesejecucion.application.service;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.ConsultarOrdenUseCase;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.EjecucionOrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.HistorialEstadoOrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.OrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EjecucionOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EstadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.HistorialEstadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.Orden;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ConsultarOrdenUseCaseImpl implements ConsultarOrdenUseCase {

    private final OrdenRepositoryPort ordenRepository;
    private final EjecucionOrdenRepositoryPort ejecucionRepository;
    private final HistorialEstadoOrdenRepositoryPort historialRepository;

    public ConsultarOrdenUseCaseImpl(OrdenRepositoryPort ordenRepository,
                                     EjecucionOrdenRepositoryPort ejecucionRepository,
                                     HistorialEstadoOrdenRepositoryPort historialRepository) {
        this.ordenRepository = ordenRepository;
        this.ejecucionRepository = ejecucionRepository;
        this.historialRepository = historialRepository;
    }

    @Override
    public Orden buscarPorId(Long id) {
        return ordenRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada: " + id));
    }

    @Override
    public List<Orden> buscarPorUsuario(Long usuarioId, EstadoOrden estado, String ticker) {
        return ordenRepository.findByUsuarioIdAndFilters(usuarioId, estado, ticker);
    }

    @Override
    public List<EjecucionOrden> buscarFillsPorOrden(Long ordenId) {
        return ejecucionRepository.findByOrdenId(ordenId);
    }

    @Override
    public List<HistorialEstadoOrden> buscarHistorialPorOrden(Long ordenId) {
        return historialRepository.findByOrdenId(ordenId);
    }
}

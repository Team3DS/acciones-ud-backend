package com.gfc.application;

import com.gfc.application.exception.RecursoNoEncontradoException;
import com.gfc.domain.model.Ingrediente;
import com.gfc.domain.model.MovimientoInventario;
import com.gfc.domain.model.TipoMovimiento;
import com.gfc.infrastructure.persistence.IngredienteRepository;
import com.gfc.infrastructure.persistence.MovimientoInventarioRepository;
import com.gfc.web.dto.IngredienteResponse;
import com.gfc.web.dto.MovimientoRequest;
import com.gfc.web.dto.MovimientoResultadoResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Visor del inventario y registro de entradas/salidas manuales de stock. */
@Service
public class InventarioService {
    private final IngredienteRepository ingredienteRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;

    public InventarioService(IngredienteRepository ingredienteRepository,
                              MovimientoInventarioRepository movimientoInventarioRepository) {
        this.ingredienteRepository = ingredienteRepository;
        this.movimientoInventarioRepository = movimientoInventarioRepository;
    }

    /** @return el stock actual de todos los ingredientes ("visor global de inventario"). */
    @Transactional(readOnly = true)
    public List<IngredienteResponse> listar() {
        return ingredienteRepository.findAll().stream().map(this::mapearIngrediente).toList();
    }

    /** Carga de inventario: suma stock. @throws RecursoNoEncontradoException si el ingrediente no existe. */
    @Transactional
    public MovimientoResultadoResponse registrarEntrada(Long ingredienteId, MovimientoRequest request) {
        return registrarMovimiento(ingredienteId, request, TipoMovimiento.ENTRADA);
    }

    /**
     * Descarga manual de inventario (no asociada a un pedido).
     *
     * @throws RecursoNoEncontradoException si el ingrediente no existe.
     * @throws IllegalStateException si no hay stock suficiente para descontar.
     */
    @Transactional
    public MovimientoResultadoResponse registrarSalida(Long ingredienteId, MovimientoRequest request) {
        return registrarMovimiento(ingredienteId, request, TipoMovimiento.SALIDA);
    }

    private MovimientoResultadoResponse registrarMovimiento(Long ingredienteId, MovimientoRequest request,
                                                              TipoMovimiento tipo) {
        Ingrediente ingrediente = ingredienteRepository.findById(ingredienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El ingrediente " + ingredienteId + " no existe"));
        LocalDateTime fecha = request.fecha() != null ? request.fecha() : LocalDateTime.now();
        MovimientoInventario movimiento = new MovimientoInventario(null, ingrediente, request.cantidad(),
                fecha, request.motivo(), tipo);
        movimiento.aplicar();
        movimientoInventarioRepository.save(movimiento);
        return new MovimientoResultadoResponse(
                new MovimientoResultadoResponse.Movimiento(movimiento.getId(), movimiento.getTipo().name(),
                        movimiento.getCantidad(), movimiento.getFecha(), movimiento.isAplicado()),
                ingrediente.getStockActual());
    }

    private IngredienteResponse mapearIngrediente(Ingrediente ingrediente) {
        return new IngredienteResponse(ingrediente.getId(), ingrediente.getNombreIngrediente(),
                ingrediente.getUnidadMedida().name(), ingrediente.getStockActual());
    }
}

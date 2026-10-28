package com.gfc.application;

import com.gfc.application.exception.ConflictoDatosException;
import com.gfc.application.exception.RecursoNoEncontradoException;
import com.gfc.domain.model.Ingrediente;
import com.gfc.domain.model.UnidadMedida;
import com.gfc.infrastructure.persistence.ConfiguracionIngredienteRepository;
import com.gfc.infrastructure.persistence.IngredienteRepository;
import com.gfc.infrastructure.persistence.MovimientoInventarioRepository;
import com.gfc.web.dto.IngredienteRequest;
import com.gfc.web.dto.IngredienteResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IngredienteService {
    private final IngredienteRepository ingredienteRepository;
    private final ConfiguracionIngredienteRepository configuracionIngredienteRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;

    public IngredienteService(IngredienteRepository ingredienteRepository,
                               ConfiguracionIngredienteRepository configuracionIngredienteRepository,
                               MovimientoInventarioRepository movimientoInventarioRepository) {
        this.ingredienteRepository = ingredienteRepository;
        this.configuracionIngredienteRepository = configuracionIngredienteRepository;
        this.movimientoInventarioRepository = movimientoInventarioRepository;
    }

    @Transactional(readOnly = true)
    public List<IngredienteResponse> listar() {
        return ingredienteRepository.findAll().stream().map(this::mapear).toList();
    }

    @Transactional
    public IngredienteResponse crear(IngredienteRequest request) {
        if (request.nombre() == null || request.nombre().isBlank()) {
            throw new IllegalArgumentException("nombre es obligatorio");
        }
        if (request.unidadMedida() == null || request.unidadMedida().isBlank()) {
            throw new IllegalArgumentException("unidadMedida es obligatoria");
        }
        if (request.stockActual() == null) {
            throw new IllegalArgumentException("stockActual es obligatorio");
        }
        UnidadMedida unidad;
        try {
            unidad = UnidadMedida.valueOf(request.unidadMedida());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unidadMedida no es valida: " + request.unidadMedida());
        }
        String nombre = request.nombre().strip();
        if (ingredienteRepository.existsByNombreIngredienteIgnoreCase(nombre)) {
            throw new ConflictoDatosException("Ya existe un ingrediente con el nombre " + nombre);
        }
        Ingrediente ingrediente = new Ingrediente(null, nombre, request.stockActual(), unidad);
        ingrediente = ingredienteRepository.save(ingrediente);
        return mapear(ingrediente);
    }

    @Transactional
    public IngredienteResponse actualizar(Long id, IngredienteRequest request) {
        Ingrediente ingrediente = ingredienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El ingrediente " + id + " no existe"));
        if (request.nombre() != null && !request.nombre().isBlank()) {
            String nuevoNombre = request.nombre().strip();
            if (ingredienteRepository.existsByNombreIngredienteIgnoreCaseAndIdNot(nuevoNombre, id)) {
                throw new ConflictoDatosException("Ya existe otro ingrediente con el nombre " + nuevoNombre);
            }
            ingrediente.actualizarNombre(nuevoNombre);
        }
        if (request.unidadMedida() != null && !request.unidadMedida().isBlank()) {
            UnidadMedida unidad;
            try {
                unidad = UnidadMedida.valueOf(request.unidadMedida());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("unidadMedida no es valida: " + request.unidadMedida());
            }
            ingrediente.actualizarUnidadMedida(unidad);
        }
        ingrediente = ingredienteRepository.save(ingrediente);
        return mapear(ingrediente);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!ingredienteRepository.existsById(id)) {
            throw new RecursoNoEncontradoException("El ingrediente " + id + " no existe");
        }
        if (configuracionIngredienteRepository.existsByIngredienteId(id)) {
            throw new ConflictoDatosException("El ingrediente no se puede eliminar: esta en uso en una receta");
        }
        if (movimientoInventarioRepository.existsByIngredienteId(id)) {
            throw new ConflictoDatosException("El ingrediente no se puede eliminar: tiene movimientos de inventario");
        }
        ingredienteRepository.deleteById(id);
    }

    private IngredienteResponse mapear(Ingrediente i) {
        return new IngredienteResponse(i.getId(), i.getNombreIngrediente(),
                i.getUnidadMedida().name(), i.getStockActual());
    }
}

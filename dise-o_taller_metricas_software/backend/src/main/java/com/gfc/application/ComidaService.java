package com.gfc.application;

import com.gfc.application.exception.ConflictoDatosException;
import com.gfc.application.exception.RecursoNoEncontradoException;
import com.gfc.domain.model.Comida;
import com.gfc.domain.model.ConfiguracionIngrediente;
import com.gfc.infrastructure.persistence.ComidaRepository;
import com.gfc.infrastructure.persistence.DetallePedidoRepository;
import com.gfc.infrastructure.persistence.IngredienteRepository;
import com.gfc.web.dto.ComidaDetalleResponse;
import com.gfc.web.dto.ComidaResumenResponse;
import com.gfc.web.dto.CrearComidaRequest;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ComidaService {
    private final ComidaRepository comidaRepository;
    private final IngredienteRepository ingredienteRepository;
    private final DetallePedidoRepository detallePedidoRepository;

    public ComidaService(ComidaRepository comidaRepository, IngredienteRepository ingredienteRepository,
                          DetallePedidoRepository detallePedidoRepository) {
        this.comidaRepository = comidaRepository;
        this.ingredienteRepository = ingredienteRepository;
        this.detallePedidoRepository = detallePedidoRepository;
    }

    @Transactional
    public ComidaResumenResponse crear(CrearComidaRequest request) {
        Comida comida = new Comida(null, request.nombre(), request.precio(), request.imagenURL());
        for (var config : request.ingredientes()) {
            var ingrediente = ingredienteRepository.findById(config.ingredienteId())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "El ingrediente " + config.ingredienteId() + " no existe"));
            comida.configurarIngrediente(null, ingrediente, config.cantidadBase(),
                    config.incluidoPorDefecto(), config.permiteAdicional(),
                    config.permiteRetirar(), config.precioAdicional());
        }
        Comida guardada = comidaRepository.save(comida);
        return new ComidaResumenResponse(guardada.getId(), guardada.getNombre(),
                guardada.getPrecio(), guardada.getImagenURL());
    }

    @Transactional
    public ComidaResumenResponse actualizar(Long id, CrearComidaRequest request) {
        Comida comida = comidaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La comida " + id + " no existe"));
        comida.actualizarNombre(request.nombre());
        comida.actualizarPrecio(request.precio());
        comida.actualizarImagenURL(request.imagenURL());
        comida.limpiarIngredientes();
        for (var config : request.ingredientes()) {
            var ingrediente = ingredienteRepository.findById(config.ingredienteId())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "El ingrediente " + config.ingredienteId() + " no existe"));
            comida.configurarIngrediente(null, ingrediente, config.cantidadBase(),
                    config.incluidoPorDefecto(), config.permiteAdicional(),
                    config.permiteRetirar(), config.precioAdicional());
        }
        Comida guardada = comidaRepository.save(comida);
        return new ComidaResumenResponse(guardada.getId(), guardada.getNombre(),
                guardada.getPrecio(), guardada.getImagenURL());
    }

    @Transactional
    public void eliminar(Long id) {
        if (!comidaRepository.existsById(id)) {
            throw new RecursoNoEncontradoException("La comida " + id + " no existe");
        }
        if (detallePedidoRepository.existsByComidaId(id)) {
            throw new ConflictoDatosException("La comida no se puede eliminar: tiene pedidos asociados");
        }
        comidaRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<ComidaResumenResponse> listar() {
        return comidaRepository.findAll().stream()
                .map(c -> new ComidaResumenResponse(c.getId(), c.getNombre(), c.getPrecio(), c.getImagenURL()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ComidaDetalleResponse obtener(Long id) {
        Comida comida = comidaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La comida " + id + " no existe"));
        List<ComidaDetalleResponse.ConfiguracionResponse> ingredientes = comida.getIngredientes().stream()
                .map(this::mapearConfiguracion)
                .toList();
        return new ComidaDetalleResponse(comida.getId(), comida.getNombre(), comida.getPrecio(),
                comida.getImagenURL(), ingredientes);
    }

    private ComidaDetalleResponse.ConfiguracionResponse mapearConfiguracion(ConfiguracionIngrediente c) {
        var ingrediente = new ComidaDetalleResponse.IngredienteResumen(c.getIngrediente().getId(),
                c.getIngrediente().getNombreIngrediente(), c.getIngrediente().getUnidadMedida().name());
        return new ComidaDetalleResponse.ConfiguracionResponse(c.getId(), ingrediente, c.getCantidadBase(),
                c.isIncluidoPorDefecto(), c.isPermiteAdicional(), c.isPermiteRetirar(), c.getPrecioAdicional());
    }
}

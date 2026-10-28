package com.gfc.application;

import com.gfc.application.exception.RecursoNoEncontradoException;
import com.gfc.application.exception.StockInsuficienteException;
import com.gfc.domain.model.Cliente;
import com.gfc.domain.model.Comida;
import com.gfc.domain.model.ConfiguracionIngrediente;
import com.gfc.domain.model.DetallePedido;
import com.gfc.domain.model.Ingrediente;
import com.gfc.domain.model.MovimientoInventario;
import com.gfc.domain.model.Pedido;
import com.gfc.domain.model.PersonalizacionIngrediente;
import com.gfc.domain.model.TipoMovimiento;
import com.gfc.infrastructure.persistence.ClienteRepository;
import com.gfc.infrastructure.persistence.ComidaRepository;
import com.gfc.infrastructure.persistence.MovimientoInventarioRepository;
import com.gfc.infrastructure.persistence.PedidoRepository;
import com.gfc.web.dto.CrearPedidoRequest;
import com.gfc.web.dto.ResumenPedidoResponse;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Crea el pedido y, en esta capa (no en el dominio), descuenta el stock consumido aplicando
 * un MovimientoInventario de SALIDA por cada ingrediente, dentro de la misma transaccion. */
@Service
public class PedidoService {
    private final ClienteRepository clienteRepository;
    private final ComidaRepository comidaRepository;
    private final PedidoRepository pedidoRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;

    public PedidoService(ClienteRepository clienteRepository, ComidaRepository comidaRepository,
                          PedidoRepository pedidoRepository,
                          MovimientoInventarioRepository movimientoInventarioRepository) {
        this.clienteRepository = clienteRepository;
        this.comidaRepository = comidaRepository;
        this.pedidoRepository = pedidoRepository;
        this.movimientoInventarioRepository = movimientoInventarioRepository;
    }

    /**
     * Arma el pedido a partir del request, valida disponibilidad, descuenta el inventario
     * consumido y persiste todo en una sola transaccion.
     *
     * @throws RecursoNoEncontradoException si el cliente, una comida o una configuracion de
     * ingrediente referenciada no existen.
     * @throws StockInsuficienteException si no hay stock para preparar el pedido completo.
     * @return el resumen de pago: lo pedido, las adiciones y los retiros ya valorados.
     */
    @Transactional
    public ResumenPedidoResponse crearPedido(String nombreUsuarioCliente, CrearPedidoRequest request) {
        Cliente cliente = clienteRepository.findByNombreUsuario(nombreUsuarioCliente)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado"));

        List<DetallePedido> detalles = request.detalles().stream().map(this::construirDetalle).toList();
        Pedido pedido = new Pedido(null, cliente, LocalDateTime.now(), detalles.get(0));
        detalles.stream().skip(1).forEach(pedido::agregarDetalle);

        if (!pedido.puedePrepararse()) {
            throw new StockInsuficienteException("No hay stock suficiente para preparar el pedido");
        }

        descontarInventario(pedido);
        pedidoRepository.save(pedido);
        return mapearResumen(pedido);
    }

    @Transactional(readOnly = true)
    public List<ResumenPedidoResponse> listarPedidos(String nombreUsuarioCliente) {
        clienteRepository.findByNombreUsuario(nombreUsuarioCliente)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado"));
        return pedidoRepository.findByClienteNombreUsuarioOrderByFechaDesc(nombreUsuarioCliente)
                .stream().map(this::mapearResumen).toList();
    }

    /** Carga la comida y arma el detalle con sus personalizaciones a partir del request. */
    private DetallePedido construirDetalle(CrearPedidoRequest.DetalleRequest request) {
        Comida comida = comidaRepository.findById(request.comidaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("La comida " + request.comidaId() + " no existe"));
        DetallePedido detalle = new DetallePedido(null, comida, request.cantidad());
        for (CrearPedidoRequest.PersonalizacionRequest p : request.personalizaciones()) {
            ConfiguracionIngrediente configuracion = comida.getIngredientes().stream()
                    .filter(c -> c.getId().equals(p.configuracionId()))
                    .findFirst()
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "La configuracion " + p.configuracionId() + " no pertenece a la comida"));
            detalle.agregarPersonalizacion(
                    new PersonalizacionIngrediente(null, configuracion, p.cantidad(), p.tipoAdicion()));
        }
        return detalle;
    }

    /** Suma el consumo de cada ingrediente entre todos los detalles y aplica una SALIDA por cada uno. */
    private void descontarInventario(Pedido pedido) {
        Map<Ingrediente, Integer> necesarias = new LinkedHashMap<>();
        for (DetallePedido detalle : pedido.getDetalle()) {
            detalle.cantidadesNecesarias().forEach((ingrediente, cantidad) ->
                    necesarias.merge(ingrediente, cantidad, Math::addExact));
        }
        LocalDateTime ahora = LocalDateTime.now();
        for (Map.Entry<Ingrediente, Integer> entrada : necesarias.entrySet()) {
            if (entrada.getValue() <= 0) continue;
            MovimientoInventario movimiento = new MovimientoInventario(null, entrada.getKey(),
                    entrada.getValue(), ahora, "Consumo de pedido", TipoMovimiento.SALIDA);
            movimiento.aplicar();
            movimientoInventarioRepository.save(movimiento);
        }
    }

    private ResumenPedidoResponse mapearResumen(Pedido pedido) {
        List<ResumenPedidoResponse.DetalleResumen> detalle = pedido.getDetalle().stream()
                .map(this::mapearDetalle)
                .toList();
        return new ResumenPedidoResponse(pedido.getId(), pedido.getFecha(), detalle, pedido.getValorTotal());
    }

    private ResumenPedidoResponse.DetalleResumen mapearDetalle(DetallePedido detalle) {
        List<ResumenPedidoResponse.PersonalizacionResumen> personalizaciones =
                detalle.getDetallePedidoIngrediente().stream()
                        .map(p -> new ResumenPedidoResponse.PersonalizacionResumen(
                                p.getComidaIngrediente().getIngrediente().getNombreIngrediente(),
                                p.getTipoAdicion().name(), p.getCantidad(), p.calcularCostoAdicional()))
                        .toList();
        return new ResumenPedidoResponse.DetalleResumen(detalle.getComida().getNombre(), detalle.getCantidad(),
                detalle.getPrecioUnitario(), personalizaciones, detalle.getSubtotal());
    }
}

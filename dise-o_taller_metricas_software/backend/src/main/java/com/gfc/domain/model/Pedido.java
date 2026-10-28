package com.gfc.domain.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * La compra de un {@link Cliente}: una fecha y uno o mas {@link DetallePedido}. Siempre conserva
 * al menos un detalle (no se puede vaciar por completo). El descuento real del inventario al
 * confirmar el pedido no ocurre aqui: lo hace la capa de servicio (ver PedidoService), que llama
 * a {@link #puedePrepararse()} para validar y luego aplica los movimientos de inventario.
 */
@Entity
@Table(name = "pedidos")
public class Pedido extends BaseEntity {
    /** Requerido por Hibernate. */
    protected Pedido() { }

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;
    @Column(nullable = false)
    private LocalDateTime fecha;
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<DetallePedido> detalle = new ArrayList<>();

    public Pedido(Long id, Cliente cliente, LocalDateTime fecha, DetallePedido primerDetalle) {
        super(id);
        this.cliente = Objects.requireNonNull(cliente, "cliente");
        this.fecha = Objects.requireNonNull(fecha, "fecha");
        agregarDetalle(primerDetalle);
    }

    /** @throws IllegalArgumentException si el detalle ya pertenece a este pedido. */
    public void agregarDetalle(DetallePedido nuevoDetalle) {
        Objects.requireNonNull(nuevoDetalle, "detalle");
        if (detalle.stream().anyMatch(d -> d == nuevoDetalle || (nuevoDetalle.getId() != null
                && nuevoDetalle.getId().equals(d.getId())))) {
            throw new IllegalArgumentException("El detalle ya pertenece al pedido");
        }
        nuevoDetalle.vincular(this);
        detalle.add(nuevoDetalle);
    }

    /** @throws IllegalStateException si es el ultimo detalle: un pedido no puede quedar vacio. */
    public void eliminarDetalle(DetallePedido detalleEliminar) {
        if (!detalle.contains(Objects.requireNonNull(detalleEliminar, "detalle"))) {
            throw new IllegalArgumentException("El detalle no pertenece al pedido");
        }
        if (detalle.size() == 1) {
            throw new IllegalStateException("Un pedido debe conservar al menos un detalle");
        }
        detalle.remove(detalleEliminar);
        detalleEliminar.desvincular();
    }

    /** @return la suma de los subtotales vigentes de cada detalle (siempre recalculado). */
    public BigDecimal calcularTotal() {
        return detalle.stream().map(DetallePedido::calcularSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * @return true si hay stock suficiente para preparar todo el pedido, agregando el consumo de
     * un mismo ingrediente compartido entre varios detalles. Solo consulta stock, no lo descuenta.
     */
    public boolean puedePrepararse() {
        Map<Ingrediente, Integer> cantidades = new LinkedHashMap<>();
        for (DetallePedido item : detalle) {
            item.cantidadesNecesarias().forEach((ingrediente, cantidad) ->
                    cantidades.merge(ingrediente, cantidad, Math::addExact));
        }
        // Un mismo ingrediente persistido puede aparecer en distintas instancias de receta.
        Map<Long, Integer> porId = new LinkedHashMap<>();
        cantidades.forEach((ingrediente, cantidad) -> {
            if (ingrediente.getId() != null) {
                porId.merge(ingrediente.getId(), cantidad, Math::addExact);
            }
        });
        return cantidades.entrySet().stream().allMatch(e -> e.getKey().hayStock(
                e.getKey().getId() == null ? e.getValue() : porId.get(e.getKey().getId())));
    }

    public Cliente getCliente() { return cliente; }
    public LocalDateTime getFecha() { return fecha; }
    public List<DetallePedido> getDetalle() { return List.copyOf(detalle); }
    public BigDecimal getValorTotal() { return calcularTotal(); }
}

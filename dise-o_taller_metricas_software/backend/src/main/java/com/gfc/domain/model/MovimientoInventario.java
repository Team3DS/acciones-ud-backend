package com.gfc.domain.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Un ajuste de stock sobre un {@link Ingrediente} (entrada, salida o ajuste con signo). Crearlo
 * no modifica el stock; hay que llamar a {@link #aplicar()} explicitamente, y solo se puede
 * aplicar una vez.
 */
@Entity
@Table(name = "movimientos_inventario")
public class MovimientoInventario extends BaseEntity {
    /** Requerido por Hibernate. */
    protected MovimientoInventario() { }

    private int cantidad;
    @Column(nullable = false)
    private LocalDateTime fecha;
    @ManyToOne(optional = false)
    @JoinColumn(name = "ingrediente_id", nullable = false)
    private Ingrediente ingrediente;
    @Column(nullable = false)
    private String motivo;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimiento tipo;
    private boolean aplicado;

    /**
     * @param cantidad para ENTRADA/SALIDA debe ser positiva; para AJUSTE es una diferencia con
     *                 signo (positiva suma, negativa resta) y no puede ser cero.
     */
    public MovimientoInventario(Long id, Ingrediente ingrediente, int cantidad,
                                LocalDateTime fecha, String motivo, TipoMovimiento tipo) {
        super(id);
        this.ingrediente = Objects.requireNonNull(ingrediente, "ingrediente");
        this.fecha = Objects.requireNonNull(fecha, "fecha");
        this.motivo = Validaciones.texto(motivo, "motivo");
        this.tipo = Objects.requireNonNull(tipo, "tipo");
        if (tipo == TipoMovimiento.AJUSTE) {
            if (cantidad == 0 || cantidad == Integer.MIN_VALUE) {
                throw new IllegalArgumentException("El ajuste debe ser una diferencia no nula representable");
            }
        } else {
            Validaciones.positivo(cantidad, "cantidad");
        }
        this.cantidad = cantidad;
    }

    /** Aplica una vez; una operacion fallida no marca el movimiento como aplicado. */
    public void aplicar() {
        if (aplicado) {
            throw new IllegalStateException("El movimiento ya fue aplicado");
        }
        switch (tipo) {
            case ENTRADA -> ingrediente.aumentarStock(cantidad);
            case SALIDA -> ingrediente.disminuirStock(cantidad);
            case AJUSTE -> {
                if (cantidad > 0) ingrediente.aumentarStock(cantidad);
                else ingrediente.disminuirStock(-cantidad);
            }
        }
        aplicado = true;
    }

    public Long getIdMovimientoInventario() { return getId(); }
    public int getCantidad() { return cantidad; }
    public LocalDateTime getFecha() { return fecha; }
    public Ingrediente getIngrediente() { return ingrediente; }
    public String getMotivo() { return motivo; }
    public TipoMovimiento getTipo() { return tipo; }
    public boolean isAplicado() { return aplicado; }
}

package com.gfc.domain.model;

import jakarta.persistence.*;

import java.util.Objects;

/**
 * Un insumo de cocina con stock propio, medido en su {@link UnidadMedida}. El stock solo cambia
 * a traves de {@link #aumentarStock(int)}/{@link #disminuirStock(int)}, tipicamente llamados
 * desde {@link MovimientoInventario#aplicar()}.
 */
@Entity
@Table(name = "ingredientes")
public class Ingrediente extends BaseEntity {
    /** Requerido por Hibernate. */
    protected Ingrediente() { }

    @Column(nullable = false)
    private String nombreIngrediente;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UnidadMedida unidadMedida;
    private int stockActual;

    public Ingrediente(Long id, String nombreIngrediente, int stockActual, UnidadMedida unidadMedida) {
        super(id);
        this.nombreIngrediente = Validaciones.texto(nombreIngrediente, "nombreIngrediente");
        this.stockActual = Validaciones.noNegativo(stockActual, "stockActual");
        this.unidadMedida = Objects.requireNonNull(unidadMedida, "unidadMedida");
    }

    /** @throws ArithmeticException si la suma desborda un int. */
    public void aumentarStock(int cantidad) {
        stockActual = Math.addExact(stockActual, Validaciones.positivo(cantidad, "cantidad"));
    }

    /** @throws IllegalStateException si no hay stock suficiente; el stock no cambia en ese caso. */
    public void disminuirStock(int cantidad) {
        Validaciones.positivo(cantidad, "cantidad");
        if (!hayStock(cantidad)) {
            throw new IllegalStateException("Stock insuficiente para " + nombreIngrediente);
        }
        stockActual -= cantidad;
    }

    /** @return true si el stock actual alcanza para {@code cantidad}; no reserva ni descuenta nada. */
    public boolean hayStock(int cantidad) {
        return stockActual >= Validaciones.noNegativo(cantidad, "cantidad");
    }

    public Long getIdIngrediente() { return getId(); }
    public String getNombreIngrediente() { return nombreIngrediente; }
    public int getStockActual() { return stockActual; }
    public UnidadMedida getUnidadMedida() { return unidadMedida; }

    public void actualizarNombre(String nombre) {
        this.nombreIngrediente = Validaciones.texto(nombre, "nombreIngrediente");
    }

    public void actualizarUnidadMedida(UnidadMedida unidad) {
        this.unidadMedida = Objects.requireNonNull(unidad, "unidadMedida");
    }
}

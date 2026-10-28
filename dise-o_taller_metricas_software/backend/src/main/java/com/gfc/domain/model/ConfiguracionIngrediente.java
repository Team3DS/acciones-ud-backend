package com.gfc.domain.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Como un {@link Ingrediente} participa en la receta de una {@link Comida}: cuanta cantidad base
 * trae por defecto y si el cliente puede agregarlo o retirarlo al personalizar un pedido.
 */
@Entity
@Table(name = "configuraciones_ingrediente", uniqueConstraints =
        @UniqueConstraint(columnNames = {"comida_id", "ingrediente_id"}))
public class ConfiguracionIngrediente extends BaseEntity {
    /** Requerido por Hibernate. */
    protected ConfiguracionIngrediente() { }

    private int cantidadBase;
    @ManyToOne(optional = false)
    @JoinColumn(name = "comida_id", nullable = false)
    private Comida comida;
    private boolean incluidoPorDefecto;
    @ManyToOne(optional = false)
    @JoinColumn(name = "ingrediente_id", nullable = false)
    private Ingrediente ingrediente;
    private boolean permiteAdicional;
    private boolean permiteRetirar;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal precioAdicional;

    // La comida controla la composicion y evita configuraciones duplicadas.
    ConfiguracionIngrediente(Long id, Comida comida, Ingrediente ingrediente, int cantidadBase,
            boolean incluidoPorDefecto, boolean permiteAdicional, boolean permiteRetirar,
            BigDecimal precioAdicional) {
        super(id);
        this.comida = Objects.requireNonNull(comida, "comida");
        this.ingrediente = Objects.requireNonNull(ingrediente, "ingrediente");
        this.cantidadBase = Validaciones.noNegativo(cantidadBase, "cantidadBase");
        if (incluidoPorDefecto && cantidadBase == 0) {
            throw new IllegalArgumentException("Un ingrediente incluido requiere cantidad base positiva");
        }
        if (permiteRetirar && !incluidoPorDefecto) {
            throw new IllegalArgumentException("Solo se pueden retirar ingredientes incluidos por defecto");
        }
        this.incluidoPorDefecto = incluidoPorDefecto;
        this.permiteAdicional = permiteAdicional;
        this.permiteRetirar = permiteRetirar;
        this.precioAdicional = Validaciones.dinero(precioAdicional, "precioAdicional");
    }

    /** @return true si esta configuracion admite el tipo de personalizacion indicado. */
    public boolean permitePersonalizacion(TipoPersonalizacion tipo) {
        return switch (Objects.requireNonNull(tipo, "tipo")) {
            case ADICIONAL -> permiteAdicional;
            case RETIRADO -> permiteRetirar && incluidoPorDefecto;
        };
    }

    public int getCantidadBase() { return cantidadBase; }
    public Comida getComida() { return comida; }
    public boolean isIncluidoPorDefecto() { return incluidoPorDefecto; }
    public Ingrediente getIngrediente() { return ingrediente; }
    public boolean isPermiteAdicional() { return permiteAdicional; }
    public boolean isPermiteRetirar() { return permiteRetirar; }
    public BigDecimal getPrecioAdicional() { return precioAdicional; }
}

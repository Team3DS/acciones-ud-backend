package com.gfc.domain.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Agregar o retirar unidades de un ingrediente de la receta de un {@link DetallePedido}. Un
 * retiro nunca cuesta ni descuenta del precio; un adicional cobra
 * {@code cantidad * precioAdicionalUnitario} (ver {@link #calcularCostoAdicional()}).
 */
@Entity
@Table(name = "personalizaciones_ingrediente", uniqueConstraints =
        @UniqueConstraint(columnNames = {"detalle_id", "configuracion_id"}))
public class PersonalizacionIngrediente extends BaseEntity {
    /** Requerido por Hibernate. */
    protected PersonalizacionIngrediente() { }

    private int cantidad;
    @ManyToOne(optional = false)
    @JoinColumn(name = "configuracion_id", nullable = false)
    private ConfiguracionIngrediente comidaIngrediente;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal precioAdicionalUnitario;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoPersonalizacion tipoAdicion;
    @ManyToOne(optional = false)
    @JoinColumn(name = "detalle_id", nullable = false)
    private DetallePedido detalle;

    /**
     * @throws IllegalArgumentException si la configuracion no permite este tipo de
     * personalizacion, o si se intenta retirar mas que la cantidad base incluida.
     */
    public PersonalizacionIngrediente(Long id, ConfiguracionIngrediente comidaIngrediente,
                                     int cantidad, TipoPersonalizacion tipoAdicion) {
        super(id);
        this.comidaIngrediente = Objects.requireNonNull(comidaIngrediente, "comidaIngrediente");
        this.cantidad = Validaciones.positivo(cantidad, "cantidad");
        this.tipoAdicion = Objects.requireNonNull(tipoAdicion, "tipoAdicion");
        if (!comidaIngrediente.permitePersonalizacion(tipoAdicion)) {
            throw new IllegalArgumentException("La configuracion no permite esta personalizacion");
        }
        if (tipoAdicion == TipoPersonalizacion.RETIRADO && cantidad > comidaIngrediente.getCantidadBase()) {
            throw new IllegalArgumentException("No se puede retirar mas que la cantidad base");
        }
        this.precioAdicionalUnitario = tipoAdicion == TipoPersonalizacion.ADICIONAL
                ? comidaIngrediente.getPrecioAdicional() : BigDecimal.ZERO;
    }

    /** @return {@code precioAdicionalUnitario * cantidad}; siempre cero para un retiro. */
    public BigDecimal calcularCostoAdicional() {
        return precioAdicionalUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    void vincular(DetallePedido detalle) {
        if (this.detalle != null && this.detalle != detalle) {
            throw new IllegalArgumentException("La personalizacion pertenece a otro detalle");
        }
        this.detalle = detalle;
    }

    void desvincular() { detalle = null; }
    public int getCantidad() { return cantidad; }
    public ConfiguracionIngrediente getComidaIngrediente() { return comidaIngrediente; }
    public BigDecimal getPrecioAdicionalUnitario() { return precioAdicionalUnitario; }
    public TipoPersonalizacion getTipoAdicion() { return tipoAdicion; }
}

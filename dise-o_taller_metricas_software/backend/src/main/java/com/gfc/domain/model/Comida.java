package com.gfc.domain.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Un producto del menu (nombre, precio en COP) cuya receta se define agregando
 * {@link ConfiguracionIngrediente} por cada ingrediente que la compone.
 */
@Entity
@Table(name = "comidas")
public class Comida extends BaseEntity {
    /** Requerido por Hibernate. */
    protected Comida() { }

    private String imagenURL;
    @Column(nullable = false)
    private String nombre;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal precio;
    @OneToMany(mappedBy = "comida", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ConfiguracionIngrediente> ingredientes = new ArrayList<>();

    public Comida(Long id, String nombre, BigDecimal precio, String imagenURL) {
        super(id);
        this.nombre = Validaciones.texto(nombre, "nombre");
        this.precio = Validaciones.dinero(precio, "precio");
        this.imagenURL = imagenURL;
    }

    /**
     * Agrega {@code ingrediente} a la receta de esta comida.
     *
     * @throws IllegalArgumentException si el ingrediente ya esta configurado en esta comida.
     */
    public ConfiguracionIngrediente configurarIngrediente(Long id, Ingrediente ingrediente,
            int cantidadBase, boolean incluidoPorDefecto, boolean permiteAdicional,
            boolean permiteRetirar, BigDecimal precioAdicional) {
        Objects.requireNonNull(ingrediente, "ingrediente");
        if (ingredientes.stream().anyMatch(c -> c.getIngrediente() == ingrediente
                || (ingrediente.getId() != null && ingrediente.getId().equals(c.getIngrediente().getId())))) {
            throw new IllegalArgumentException("El ingrediente ya esta configurado en esta comida");
        }
        ConfiguracionIngrediente configuracion = new ConfiguracionIngrediente(id, this, ingrediente,
                cantidadBase, incluidoPorDefecto, permiteAdicional, permiteRetirar, precioAdicional);
        ingredientes.add(configuracion);
        return configuracion;
    }

    public String getImagenURL() { return imagenURL; }
    public String getNombre() { return nombre; }
    public BigDecimal getPrecio() { return precio; }
    public List<ConfiguracionIngrediente> getIngredientes() { return List.copyOf(ingredientes); }

    public void actualizarNombre(String nombre) {
        this.nombre = Validaciones.texto(nombre, "nombre");
    }

    public void actualizarPrecio(BigDecimal precio) {
        this.precio = Validaciones.dinero(precio, "precio");
    }

    public void actualizarImagenURL(String imagenURL) {
        this.imagenURL = imagenURL;
    }

    public void limpiarIngredientes() {
        ingredientes.clear();
    }
}

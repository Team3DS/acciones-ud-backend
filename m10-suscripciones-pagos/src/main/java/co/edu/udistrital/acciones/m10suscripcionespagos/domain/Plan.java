package co.edu.udistrital.acciones.m10suscripcionespagos.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "plan")
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false)
    private boolean activo;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PrecioPlan> precios = new ArrayList<>();

    protected Plan() {
    }

    public static Plan crear(String codigo, String nombre, String descripcion) {
        Plan plan = new Plan();
        plan.codigo = requerido(codigo, "El codigo del plan es obligatorio");
        plan.nombre = requerido(nombre, "El nombre del plan es obligatorio");
        plan.descripcion = descripcion;
        plan.activo = true;
        return plan;
    }

    public void agregarPrecio(PrecioPlan precio) {
        Objects.requireNonNull(precio, "El precio es obligatorio");
        if (!precios.contains(precio)) {
            precios.add(precio);
        }
        precio.vincular(this);
    }

    public List<PrecioPlan> preciosActivos() {
        return precios.stream()
                .filter(PrecioPlan::isActivo)
                .sorted(Comparator.comparing(PrecioPlan::getIntervalo))
                .toList();
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean isActivo() {
        return activo;
    }

    public List<PrecioPlan> getPrecios() {
        return precios;
    }

    private static String requerido(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensaje);
        }
        return valor.strip();
    }
}

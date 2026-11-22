package co.edu.udistrital.acciones.m10suscripcionespagos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "precio_plan")
public class PrecioPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private IntervaloPlan intervalo;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monto;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3)
    private String moneda;

    @Column(name = "stripe_lookup_key", nullable = false, length = 100)
    private String stripeLookupKey;

    @Column(nullable = false)
    private boolean activo;

    protected PrecioPlan() {
    }

    public static PrecioPlan crear(Plan plan, IntervaloPlan intervalo, BigDecimal monto,
                                   String moneda, String lookupKey) {
        Objects.requireNonNull(plan, "El plan es obligatorio");
        Objects.requireNonNull(intervalo, "El intervalo es obligatorio");
        if (lookupKey == null || lookupKey.isBlank()) {
            throw new IllegalArgumentException("La clave de precio es obligatoria");
        }
        Dinero dinero = new Dinero(monto, moneda);
        PrecioPlan precio = new PrecioPlan();
        precio.intervalo = intervalo;
        precio.monto = dinero.monto();
        precio.moneda = dinero.moneda();
        precio.stripeLookupKey = lookupKey.strip();
        precio.activo = true;
        plan.agregarPrecio(precio);
        return precio;
    }

    void vincular(Plan plan) {
        this.plan = plan;
    }

    public Dinero dinero() {
        return new Dinero(monto, moneda);
    }

    public Long getId() {
        return id;
    }

    public Plan getPlan() {
        return plan;
    }

    public IntervaloPlan getIntervalo() {
        return intervalo;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public String getMoneda() {
        return moneda;
    }

    public String getStripeLookupKey() {
        return stripeLookupKey;
    }

    public boolean isActivo() {
        return activo;
    }
}

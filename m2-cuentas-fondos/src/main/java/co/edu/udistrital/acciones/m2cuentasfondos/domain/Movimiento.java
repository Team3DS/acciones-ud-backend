package co.edu.udistrital.acciones.m2cuentasfondos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "movimiento")
public class Movimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cuenta_id", nullable = false)
    private Long cuentaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoMovimiento tipo;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monto;

    @Column(name = "saldo_posterior", nullable = false, precision = 19, scale = 2)
    private BigDecimal saldoPosterior;

    @Column(length = 100)
    private String referencia;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String descripcion;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    protected Movimiento() {
    }

    public static Movimiento crear(Long cuentaId, TipoMovimiento tipo, BigDecimal monto, BigDecimal saldoPosterior,
                                   String referencia, String idempotencyKey, String descripcion, Instant creadoEn) {
        Movimiento movimiento = new Movimiento();
        movimiento.cuentaId = Objects.requireNonNull(cuentaId, "La cuenta es obligatoria");
        movimiento.tipo = Objects.requireNonNull(tipo, "El tipo de movimiento es obligatorio");
        movimiento.monto = MonetaryUtil.validarPositivo(monto);
        movimiento.saldoPosterior = MonetaryUtil.normalizar(saldoPosterior);
        movimiento.referencia = referencia;
        movimiento.idempotencyKey = Objects.requireNonNull(idempotencyKey, "La clave de idempotencia es obligatoria");
        movimiento.descripcion = descripcion;
        movimiento.creadoEn = ahora(creadoEn);
        return movimiento;
    }

    private static Instant ahora(Instant creadoEn) {
        return Objects.requireNonNull(creadoEn, "El instante de creación es obligatorio");
    }

    public Long getId() {
        return id;
    }

    public Long getCuentaId() {
        return cuentaId;
    }

    public TipoMovimiento getTipo() {
        return tipo;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public BigDecimal getSaldoPosterior() {
        return saldoPosterior;
    }

    public String getReferencia() {
        return referencia;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}

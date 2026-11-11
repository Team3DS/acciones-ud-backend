package co.edu.udistrital.acciones.m2cuentasfondos.domain;

import co.edu.udistrital.acciones.m2cuentasfondos.domain.excepcion.CuentaNoOperableException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "cuenta")
public class Cuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "saldo_contable", nullable = false, precision = 19, scale = 2)
    private BigDecimal saldoContable;

    @Column(name = "saldo_disponible", nullable = false, precision = 19, scale = 2)
    private BigDecimal saldoDisponible;

    @Column(name = "saldo_reservado", nullable = false, precision = 19, scale = 2)
    private BigDecimal saldoReservado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCuenta estado;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    protected Cuenta() {
    }

    public static Cuenta crear(Long userId, Instant ahora) {
        Cuenta cuenta = new Cuenta();
        cuenta.userId = Objects.requireNonNull(userId, "El usuario es obligatorio");
        cuenta.saldoContable = MonetaryUtil.cero();
        cuenta.saldoDisponible = MonetaryUtil.cero();
        cuenta.saldoReservado = MonetaryUtil.cero();
        cuenta.estado = EstadoCuenta.ACTIVA;
        cuenta.creadoEn = ahora;
        cuenta.actualizadoEn = ahora;
        return cuenta;
    }

    public void abonar(BigDecimal monto, Instant ahora) {
        verificarOperable();
        BigDecimal normalizado = MonetaryUtil.validarPositivo(monto);
        this.saldoContable = this.saldoContable.add(normalizado);
        this.saldoDisponible = this.saldoDisponible.add(normalizado);
        this.actualizadoEn = ahora;
    }

    public void verificarOperable() {
        if (estado != EstadoCuenta.ACTIVA) {
            throw new CuentaNoOperableException("La cuenta no está operativa: " + estado);
        }
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public BigDecimal getSaldoContable() {
        return saldoContable;
    }

    public BigDecimal getSaldoDisponible() {
        return saldoDisponible;
    }

    public BigDecimal getSaldoReservado() {
        return saldoReservado;
    }

    public EstadoCuenta getEstado() {
        return estado;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }
}

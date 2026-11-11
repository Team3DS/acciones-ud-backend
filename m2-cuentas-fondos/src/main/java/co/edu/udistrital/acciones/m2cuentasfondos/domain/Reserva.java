package co.edu.udistrital.acciones.m2cuentasfondos.domain;

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
@Table(name = "reserva")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cuenta_id", nullable = false)
    private Long cuentaId;

    @Column(name = "order_id", nullable = false, unique = true, length = 100)
    private String orderId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoReserva estado;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "execution_id", length = 100)
    private String executionId;

    @Column(name = "motivo_liberacion", length = 255)
    private String motivoLiberacion;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "confirmado_en")
    private Instant confirmadoEn;

    @Column(name = "liberado_en")
    private Instant liberadoEn;

    protected Reserva() {
    }

    public static Reserva crear(Long cuentaId, String orderId, BigDecimal monto, String idempotencyKey,
                                Instant creadoEn) {
        Reserva reserva = new Reserva();
        reserva.cuentaId = Objects.requireNonNull(cuentaId, "La cuenta es obligatoria");
        reserva.orderId = Objects.requireNonNull(orderId, "La orden es obligatoria");
        reserva.monto = MonetaryUtil.validarPositivo(monto);
        reserva.estado = EstadoReserva.PENDING;
        reserva.idempotencyKey = Objects.requireNonNull(idempotencyKey, "La clave de idempotencia es obligatoria");
        reserva.creadoEn = Objects.requireNonNull(creadoEn, "El instante de creación es obligatorio");
        return reserva;
    }

    public Long getId() {
        return id;
    }

    public Long getCuentaId() {
        return cuentaId;
    }

    public String getOrderId() {
        return orderId;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getExecutionId() {
        return executionId;
    }

    public String getMotivoLiberacion() {
        return motivoLiberacion;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public Instant getConfirmadoEn() {
        return confirmadoEn;
    }

    public Instant getLiberadoEn() {
        return liberadoEn;
    }
}

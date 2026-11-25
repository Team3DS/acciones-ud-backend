package co.edu.udistrital.acciones.m2cuentasfondos.domain;

import co.edu.udistrital.acciones.m2cuentasfondos.domain.excepcion.TransicionReservaInvalidaException;
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

    @Column(name = "monto_consumido", nullable = false, precision = 19, scale = 2)
    private BigDecimal montoConsumido = MonetaryUtil.cero();

    @Column(name = "ejecuciones_aplicadas", nullable = false)
    private int ejecucionesAplicadas;

    @Column(name = "ejecuciones_esperadas")
    private Integer ejecucionesEsperadas;

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

    public void confirmar(String executionId, Instant ahora) {
        if (estado != EstadoReserva.PENDING) {
            throw new TransicionReservaInvalidaException(
                    "No se puede confirmar una reserva en estado " + estado);
        }
        String ejecucion = Objects.requireNonNull(executionId, "El executionId es obligatorio");
        Instant instante = Objects.requireNonNull(ahora, "El instante de confirmación es obligatorio");
        this.executionId = ejecucion;
        this.estado = EstadoReserva.CONFIRMED;
        this.confirmadoEn = instante;
    }

    public boolean completaConEjecucion(boolean esFinal, Integer esperadas) {
        if (esperadas != null && (!esFinal || esperadas < ejecucionesAplicadas + 1)) {
            throw new TransicionReservaInvalidaException("Cantidad esperada de ejecuciones incompatible");
        }
        if (esFinal && ejecucionesEsperadas != null) {
            throw new TransicionReservaInvalidaException("La orden ya tiene una ejecución final");
        }
        Integer total = ejecucionesEsperadas;
        if (esFinal) {
            total = esperadas == null ? Integer.valueOf(ejecucionesAplicadas + 1) : esperadas;
        }
        return total != null && ejecucionesAplicadas + 1 == total;
    }

    public void aplicarEjecucion(String executionId, BigDecimal ejecutado, boolean esFinal,
                                Integer esperadas, Instant ahora) {
        if (estado != EstadoReserva.PENDING) {
            throw new TransicionReservaInvalidaException(
                    "No se puede ejecutar una reserva en estado " + estado);
        }
        boolean completa = completaConEjecucion(esFinal, esperadas);
        if (esFinal) {
            this.executionId = Objects.requireNonNull(executionId, "El executionId es obligatorio");
            this.ejecucionesEsperadas = esperadas == null ? ejecucionesAplicadas + 1 : esperadas;
        }
        this.montoConsumido = this.montoConsumido.add(MonetaryUtil.validarPositivo(ejecutado));
        this.ejecucionesAplicadas++;
        if (completa) {
            confirmar(this.executionId, ahora);
        }
    }

    public BigDecimal getMontoConsumido() {
        return montoConsumido;
    }

    public BigDecimal getMontoPendiente() {
        return estado == EstadoReserva.PENDING
                ? monto.subtract(montoConsumido).max(MonetaryUtil.cero()) : MonetaryUtil.cero();
    }

    public void liberar(String motivo, Instant ahora) {
        if (estado != EstadoReserva.PENDING) {
            throw new TransicionReservaInvalidaException(
                    "No se puede liberar una reserva en estado " + estado);
        }
        Instant instante = Objects.requireNonNull(ahora, "El instante de liberación es obligatorio");
        this.estado = EstadoReserva.RELEASED;
        this.motivoLiberacion = motivo;
        this.liberadoEn = instante;
    }

    public void expirar(Instant ahora) {
        if (estado != EstadoReserva.PENDING) {
            throw new TransicionReservaInvalidaException(
                    "No se puede expirar una reserva en estado " + estado);
        }
        Instant instante = Objects.requireNonNull(ahora, "El instante de expiración es obligatorio");
        this.estado = EstadoReserva.EXPIRED;
        this.motivoLiberacion = "EXPIRADA";
        this.liberadoEn = instante;
    }

    public boolean estaVencida(Instant limite) {
        Instant instante = Objects.requireNonNull(limite, "El límite de expiración es obligatorio");
        // Una orden parcialmente ejecutada sigue abierta en el broker. Solo su cierre libera el remanente.
        return estado == EstadoReserva.PENDING && montoConsumido.signum() == 0 && creadoEn.isBefore(instante);
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

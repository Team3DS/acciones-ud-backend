package co.edu.udistrital.acciones.m6portafolio.domain;

import co.edu.udistrital.acciones.m6portafolio.domain.excepcion.TransicionReservaException;
import co.edu.udistrital.acciones.m6portafolio.domain.excepcion.VentaNoDisponibleException;
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
@Table(name = "reserva_posicion")
public class ReservaPosicion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "posicion_id", nullable = false)
    private Long posicionId;

    @Column(name = "order_id", nullable = false, unique = true, length = 100)
    private String orderId;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal cantidad;

    @Column(name = "cantidad_ejecutada", nullable = false, precision = 19, scale = 8)
    private BigDecimal cantidadEjecutada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoReservaPosicion estado;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 110)
    private String idempotencyKey;

    @Column(name = "motivo_liberacion", length = 255)
    private String motivoLiberacion;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "confirmado_en")
    private Instant confirmadoEn;

    @Column(name = "liberado_en")
    private Instant liberadoEn;

    protected ReservaPosicion() {
    }

    public static ReservaPosicion crear(Long posicionId, String orderId, BigDecimal cantidad,
                                        String idempotencyKey, Instant creadoEn) {
        ReservaPosicion reserva = new ReservaPosicion();
        reserva.posicionId = Objects.requireNonNull(posicionId, "La posicion es obligatoria");
        reserva.orderId = exigirTexto(orderId, "La orden es obligatoria");
        reserva.cantidad = Cantidades.validarPositiva(cantidad);
        reserva.cantidadEjecutada = Cantidades.cero();
        reserva.estado = EstadoReservaPosicion.PENDING;
        reserva.idempotencyKey = exigirTexto(idempotencyKey, "La clave de idempotencia es obligatoria");
        reserva.creadoEn = Objects.requireNonNull(creadoEn, "El instante de creacion es obligatorio");
        return reserva;
    }

    public BigDecimal pendiente() {
        return this.cantidad.subtract(this.cantidadEjecutada);
    }

    public BigDecimal liberar(String motivo, Instant ahora) {
        if (this.estado != EstadoReservaPosicion.PENDING) {
            throw new TransicionReservaException(
                    "No se puede liberar una reserva en estado " + this.estado);
        }
        Instant instante = Objects.requireNonNull(ahora, "El instante de liberacion es obligatorio");
        BigDecimal pendiente = pendiente();
        this.estado = EstadoReservaPosicion.RELEASED;
        this.motivoLiberacion = motivo;
        this.liberadoEn = instante;
        return pendiente;
    }

    public void ejecutar(BigDecimal cantidad, Instant ahora) {
        if (this.estado != EstadoReservaPosicion.PENDING) {
            throw new TransicionReservaException(
                    "No se puede ejecutar una reserva en estado " + this.estado);
        }
        BigDecimal ejecucion = Cantidades.validarPositiva(cantidad);
        if (pendiente().compareTo(ejecucion) < 0) {
            throw new VentaNoDisponibleException("La venta supera la cantidad pendiente de la reserva");
        }
        Instant instante = Objects.requireNonNull(ahora, "El instante de ejecucion es obligatorio");
        this.cantidadEjecutada = this.cantidadEjecutada.add(ejecucion);
        if (pendiente().signum() == 0) {
            this.estado = EstadoReservaPosicion.CONFIRMED;
            this.confirmadoEn = instante;
        }
    }

    private static String exigirTexto(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensaje);
        }
        return valor;
    }

    public Long getId() {
        return id;
    }

    public Long getPosicionId() {
        return posicionId;
    }

    public String getOrderId() {
        return orderId;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public BigDecimal getCantidadEjecutada() {
        return cantidadEjecutada;
    }

    public EstadoReservaPosicion getEstado() {
        return estado;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
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

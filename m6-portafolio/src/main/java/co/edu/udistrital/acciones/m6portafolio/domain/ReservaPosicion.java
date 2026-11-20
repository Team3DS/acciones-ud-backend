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

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "reserva_posicion")
public class ReservaPosicion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "posicion_id", nullable = false)
    private Long posicionId;

    @Column(name = "orden_id", nullable = false, unique = true)
    private Long ordenId;

    @Column(name = "cantidad_reservada", nullable = false)
    private Integer cantidadReservada;

    @Column(name = "cantidad_consumida", nullable = false)
    private Integer cantidadConsumida;

    @Column(name = "cantidad_liberada", nullable = false)
    private Integer cantidadLiberada;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_reserva", nullable = false, length = 30)
    private EstadoReservaPosicion estado;

    @Column(name = "clave_idempotencia", nullable = false, unique = true, columnDefinition = "UUID")
    private UUID claveIdempotencia;

    @Column(name = "motivo_liberacion", length = 255)
    private String motivoLiberacion;

    @Column(name = "creada_en", nullable = false)
    private Instant creadaEn;

    @Column(name = "liberada_en")
    private Instant liberadaEn;

    protected ReservaPosicion() {
    }

    public static ReservaPosicion crear(Long posicionId, Long ordenId, Integer cantidad,
                                        UUID claveIdempotencia, Instant creadaEn) {
        ReservaPosicion reserva = new ReservaPosicion();
        reserva.posicionId = Objects.requireNonNull(posicionId, "La posicion es obligatoria");
        reserva.ordenId = Objects.requireNonNull(ordenId, "La orden es obligatoria");
        reserva.cantidadReservada = validarPositiva(cantidad, "La cantidad debe ser mayor que cero");
        reserva.cantidadConsumida = 0;
        reserva.cantidadLiberada = 0;
        reserva.estado = EstadoReservaPosicion.ACTIVA;
        reserva.claveIdempotencia = Objects.requireNonNull(claveIdempotencia, "La clave de idempotencia es obligatoria");
        reserva.creadaEn = Objects.requireNonNull(creadaEn, "El instante de creacion es obligatorio");
        return reserva;
    }

    public Integer pendiente() {
        return this.cantidadReservada - this.cantidadConsumida - this.cantidadLiberada;
    }

    public void consumir(Long ejecucionId, Integer cantidad) {
        if (this.estado != EstadoReservaPosicion.ACTIVA) {
            throw new TransicionReservaException(
                    "No se puede consumir una reserva en estado " + this.estado);
        }
        Integer consumo = validarPositiva(cantidad, "La cantidad a consumir debe ser mayor que cero");
        if (pendiente() < consumo) {
            throw new VentaNoDisponibleException("El consumo supera la cantidad pendiente de la reserva");
        }
        this.cantidadConsumida += consumo;
        if (pendiente() == 0) {
            this.estado = EstadoReservaPosicion.CONSUMIDA;
        }
    }

    public void liberar(String motivo, Instant ahora) {
        if (this.estado != EstadoReservaPosicion.ACTIVA) {
            throw new TransicionReservaException(
                    "No se puede liberar una reserva en estado " + this.estado);
        }
        Instant instante = Objects.requireNonNull(ahora, "El instante de liberacion es obligatorio");
        Integer pendiente = pendiente();
        this.cantidadLiberada += pendiente;
        this.estado = EstadoReservaPosicion.LIBERADA;
        this.motivoLiberacion = motivo;
        this.liberadaEn = instante;
    }

    private static Integer validarPositiva(Integer cantidad, String mensaje) {
        if (cantidad == null || cantidad <= 0) {
            throw new IllegalArgumentException(mensaje);
        }
        return cantidad;
    }

    public Long getId() {
        return id;
    }

    public Long getPosicionId() {
        return posicionId;
    }

    public Long getOrdenId() {
        return ordenId;
    }

    public Integer getCantidadReservada() {
        return cantidadReservada;
    }

    public Integer getCantidadConsumida() {
        return cantidadConsumida;
    }

    public Integer getCantidadLiberada() {
        return cantidadLiberada;
    }

    public EstadoReservaPosicion getEstado() {
        return estado;
    }

    public UUID getClaveIdempotencia() {
        return claveIdempotencia;
    }

    public String getMotivoLiberacion() {
        return motivoLiberacion;
    }

    public Instant getCreadaEn() {
        return creadaEn;
    }

    public Instant getLiberadaEn() {
        return liberadaEn;
    }
}
package co.edu.udistrital.acciones.m6portafolio.domain;

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
import java.util.UUID;

@Entity
@Table(name = "ejecucion_aplicada")
public class EjecucionAplicada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "posicion_id", nullable = false)
    private Long posicionId;

    @Column(name = "ejecucion_orden_id", nullable = false, unique = true)
    private Long ejecucionOrdenId;

    @Enumerated(EnumType.STRING)
    @Column(name = "lado_orden", nullable = false, length = 10)
    private LadoEjecucion ladoOrden;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal cantidad;

    @Column(name = "precio_ejecucion", nullable = false, precision = 19, scale = 4)
    private BigDecimal precioEjecucion;

    @Column(name = "precio_promedio_costo", nullable = false, precision = 19, scale = 4)
    private BigDecimal precioPromedioCosto;

    @Column(name = "clave_idempotencia", nullable = false, unique = true, columnDefinition = "UUID")
    private UUID claveIdempotencia;

    @Column(name = "aplicado_en", nullable = false)
    private Instant aplicadoEn;

    protected EjecucionAplicada() {
    }

    public EjecucionAplicada(Long posicionId, Long ejecucionOrdenId, LadoEjecucion ladoOrden,
                              BigDecimal cantidad, BigDecimal precioEjecucion, BigDecimal precioPromedioCosto,
                              UUID claveIdempotencia, Instant aplicadoEn) {
        this.posicionId = Objects.requireNonNull(posicionId, "posicionId es obligatorio");
        this.ejecucionOrdenId = Objects.requireNonNull(ejecucionOrdenId, "ejecucionOrdenId es obligatorio");
        this.ladoOrden = Objects.requireNonNull(ladoOrden, "ladoOrden es obligatorio");
        this.cantidad = Objects.requireNonNull(cantidad, "cantidad es obligatoria");
        this.precioEjecucion = Objects.requireNonNull(precioEjecucion, "precioEjecucion es obligatorio");
        this.precioPromedioCosto = Objects.requireNonNull(precioPromedioCosto, "precioPromedioCosto es obligatorio");
        this.claveIdempotencia = Objects.requireNonNull(claveIdempotencia, "claveIdempotencia es obligatoria");
        this.aplicadoEn = Objects.requireNonNull(aplicadoEn, "aplicadoEn es obligatorio");
    }

    public Long getId() {
        return id;
    }

    public Long getPosicionId() {
        return posicionId;
    }

    public Long getEjecucionOrdenId() {
        return ejecucionOrdenId;
    }

    public LadoEjecucion getLadoOrden() {
        return ladoOrden;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public BigDecimal getPrecioEjecucion() {
        return precioEjecucion;
    }

    public BigDecimal getPrecioPromedioCosto() {
        return precioPromedioCosto;
    }

    public UUID getClaveIdempotencia() {
        return claveIdempotencia;
    }

    public Instant getAplicadoEn() {
        return aplicadoEn;
    }
}
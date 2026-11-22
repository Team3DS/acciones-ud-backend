package co.edu.udistrital.acciones.m10suscripcionespagos.domain;

import co.edu.udistrital.acciones.m10suscripcionespagos.domain.excepcion.NegocioException;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.excepcion.TransicionSuscripcionException;
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
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "suscripcion")
public class Suscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "precio_plan_id", nullable = false)
    private PrecioPlan precioPlan;

    @Column(name = "stripe_subscription_id", nullable = false, length = 100)
    private String stripeSubscriptionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSuscripcion estado;

    @Column(name = "periodo_inicio", nullable = false)
    private Instant periodoInicio;

    @Column(name = "periodo_fin", nullable = false)
    private Instant periodoFin;

    @Column(name = "cancelar_al_final", nullable = false)
    private boolean cancelarAlFinal;

    @Column(name = "cancelada_en")
    private Instant canceladaEn;

    @Column(name = "finalizada_en")
    private Instant finalizadaEn;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    @Version
    @Column(nullable = false)
    private Long version;

    protected Suscripcion() {
    }

    public static Suscripcion crear(Long userId, DatosSuscripcion datos, Instant ahora) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("El usuario debe ser positivo");
        }
        validarDatos(datos);
        Instant instante = Objects.requireNonNull(ahora, "El instante de creacion es obligatorio");
        Suscripcion suscripcion = new Suscripcion();
        suscripcion.userId = userId;
        suscripcion.stripeSubscriptionId = datos.stripeSubscriptionId().strip();
        suscripcion.precioPlan = datos.precio();
        suscripcion.estado = datos.estado();
        suscripcion.periodoInicio = datos.periodoInicio();
        suscripcion.periodoFin = datos.periodoFin();
        suscripcion.cancelarAlFinal = datos.cancelarAlFinal();
        suscripcion.canceladaEn = datos.canceladaEn();
        suscripcion.finalizadaEn = datos.finalizadaEn() != null
                ? datos.finalizadaEn()
                : (datos.estado().esTerminal() ? instante : null);
        suscripcion.creadoEn = instante;
        suscripcion.actualizadoEn = instante;
        return suscripcion;
    }

    public TransicionSuscripcion sincronizar(DatosSuscripcion datos, Instant ahora) {
        validarDatos(datos);
        Instant instante = Objects.requireNonNull(ahora, "El instante de sincronizacion es obligatorio");
        EstadoSuscripcion anterior = this.estado;
        EstadoSuscripcion nuevo = datos.estado();
        if (anterior != null && !anterior.puedeTransicionarA(nuevo)) {
            throw new TransicionSuscripcionException(
                    "No se puede pasar de " + anterior + " a " + nuevo);
        }
        this.stripeSubscriptionId = datos.stripeSubscriptionId().strip();
        this.precioPlan = datos.precio();
        this.estado = nuevo;
        this.periodoInicio = datos.periodoInicio();
        this.periodoFin = datos.periodoFin();
        this.cancelarAlFinal = datos.cancelarAlFinal();
        if (datos.canceladaEn() != null) {
            this.canceladaEn = datos.canceladaEn();
        }
        if (datos.finalizadaEn() != null) {
            this.finalizadaEn = datos.finalizadaEn();
        } else if (nuevo.esTerminal() && this.finalizadaEn == null) {
            this.finalizadaEn = instante;
        }
        this.actualizadoEn = instante;
        return new TransicionSuscripcion(anterior, nuevo);
    }

    public boolean otorgaPremium(Instant ahora) {
        Objects.requireNonNull(ahora, "El instante de evaluacion es obligatorio");
        return estado != null && estado.otorgaPremium()
                && periodoFin != null && periodoFin.isAfter(ahora);
    }

    private static void validarDatos(DatosSuscripcion datos) {
        Objects.requireNonNull(datos, "Los datos de la suscripcion son obligatorios");
        if (datos.stripeSubscriptionId() == null || datos.stripeSubscriptionId().isBlank()) {
            throw new NegocioException("La suscripcion de Stripe es obligatoria");
        }
        Objects.requireNonNull(datos.precio(), "El precio es obligatorio");
        Objects.requireNonNull(datos.estado(), "El estado es obligatorio");
        Objects.requireNonNull(datos.periodoInicio(), "El inicio del periodo es obligatorio");
        Objects.requireNonNull(datos.periodoFin(), "El fin del periodo es obligatorio");
        if (!datos.periodoFin().isAfter(datos.periodoInicio())) {
            throw new NegocioException("El fin del periodo debe ser posterior al inicio");
        }
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public PrecioPlan getPrecioPlan() {
        return precioPlan;
    }

    public String getStripeSubscriptionId() {
        return stripeSubscriptionId;
    }

    public EstadoSuscripcion getEstado() {
        return estado;
    }

    public Instant getPeriodoInicio() {
        return periodoInicio;
    }

    public Instant getPeriodoFin() {
        return periodoFin;
    }

    public boolean isCancelarAlFinal() {
        return cancelarAlFinal;
    }

    public Instant getCanceladaEn() {
        return canceladaEn;
    }

    public Instant getFinalizadaEn() {
        return finalizadaEn;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }

    public Long getVersion() {
        return version;
    }
}

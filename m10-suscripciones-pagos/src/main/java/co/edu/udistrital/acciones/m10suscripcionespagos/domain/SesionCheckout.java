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

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "sesion_checkout")
public class SesionCheckout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "precio_plan_id", nullable = false)
    private PrecioPlan precioPlan;

    @Column(name = "stripe_session_id", nullable = false, length = 100)
    private String stripeSessionId;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(nullable = false)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSesionCheckout estado;

    @Column(name = "idempotency_key", nullable = false, length = 64)
    private String idempotencyKey;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    @Column(name = "completado_en")
    private Instant completadoEn;

    @Column(name = "stripe_customer_id", length = 100)
    private String stripeCustomerId;

    protected SesionCheckout() {
    }

    public static SesionCheckout crear(Long userId, PrecioPlan precioPlan, String stripeSessionId,
                                       String url, String idempotencyKey, Instant creadoEn, Instant expiraEn) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("El usuario debe ser positivo");
        }
        if (stripeSessionId == null || stripeSessionId.isBlank()) {
            throw new IllegalArgumentException("La sesion de Stripe es obligatoria");
        }
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("La url de pago es obligatoria");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 64) {
            throw new IllegalArgumentException("La clave de idempotencia no es valida");
        }
        Instant creacion = Objects.requireNonNull(creadoEn, "El instante de creacion es obligatorio");
        Instant expiracion = Objects.requireNonNull(expiraEn, "El instante de expiracion es obligatorio");
        if (!expiracion.isAfter(creacion)) {
            throw new IllegalArgumentException("La expiracion debe ser posterior a la creacion");
        }
        SesionCheckout sesion = new SesionCheckout();
        sesion.userId = userId;
        sesion.precioPlan = Objects.requireNonNull(precioPlan, "El precio es obligatorio");
        sesion.stripeSessionId = stripeSessionId.strip();
        sesion.url = url.strip();
        sesion.estado = EstadoSesionCheckout.ABIERTA;
        sesion.idempotencyKey = idempotencyKey.strip();
        sesion.creadoEn = creacion;
        sesion.expiraEn = expiracion;
        return sesion;
    }

    public boolean estaVigente(Instant ahora) {
        return estado == EstadoSesionCheckout.ABIERTA && expiraEn.isAfter(ahora);
    }

    public boolean esDelPrecio(PrecioPlan precio) {
        if (precio == null) {
            return false;
        }
        if (precioPlan == null) {
            return false;
        }
        if (precioPlan.getId() != null && precio.getId() != null) {
            return precioPlan.getId().equals(precio.getId());
        }
        return precioPlan == precio;
    }

    public void marcarCompletada(String customerId, Instant ahora) {
        if (estado != EstadoSesionCheckout.ABIERTA) {
            return;
        }
        estado = EstadoSesionCheckout.COMPLETADA;
        completadoEn = Objects.requireNonNull(ahora, "El instante de finalizacion es obligatorio");
        if (customerId != null && !customerId.isBlank()) {
            stripeCustomerId = customerId.strip();
        }
    }

    public void marcarExpirada() {
        if (estado == EstadoSesionCheckout.ABIERTA) {
            estado = EstadoSesionCheckout.EXPIRADA;
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

    public String getStripeSessionId() {
        return stripeSessionId;
    }

    public String getUrl() {
        return url;
    }

    public EstadoSesionCheckout getEstado() {
        return estado;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public Instant getExpiraEn() {
        return expiraEn;
    }

    public Instant getCompletadoEn() {
        return completadoEn;
    }

    public String getStripeCustomerId() {
        return stripeCustomerId;
    }
}

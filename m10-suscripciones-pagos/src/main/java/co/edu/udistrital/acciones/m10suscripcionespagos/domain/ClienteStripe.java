package co.edu.udistrital.acciones.m10suscripcionespagos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "cliente_stripe")
public class ClienteStripe {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "stripe_customer_id", nullable = false, length = 100)
    private String stripeCustomerId;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    protected ClienteStripe() {
    }

    public static ClienteStripe crear(Long userId, String stripeCustomerId, Instant ahora) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("El usuario debe ser positivo");
        }
        if (stripeCustomerId == null || stripeCustomerId.isBlank()) {
            throw new IllegalArgumentException("El cliente de Stripe es obligatorio");
        }
        ClienteStripe cliente = new ClienteStripe();
        cliente.userId = userId;
        cliente.stripeCustomerId = stripeCustomerId.strip();
        cliente.creadoEn = Objects.requireNonNull(ahora, "El instante de creacion es obligatorio");
        return cliente;
    }

    public Long getUserId() {
        return userId;
    }

    public String getStripeCustomerId() {
        return stripeCustomerId;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}

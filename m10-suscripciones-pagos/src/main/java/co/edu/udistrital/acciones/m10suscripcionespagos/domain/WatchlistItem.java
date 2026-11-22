package co.edu.udistrital.acciones.m10suscripcionespagos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "watchlist_item")
public class WatchlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    @Column(name = "agregado_en", nullable = false)
    private Instant agregadoEn;

    protected WatchlistItem() {
    }

    public static WatchlistItem crear(Long userId, Simbolo simbolo, Instant ahora) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("El usuario debe ser positivo");
        }
        Objects.requireNonNull(simbolo, "El simbolo es obligatorio");
        WatchlistItem item = new WatchlistItem();
        item.userId = userId;
        item.symbol = simbolo.valor();
        item.agregadoEn = Objects.requireNonNull(ahora, "La fecha de alta es obligatoria");
        return item;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getSymbol() {
        return symbol;
    }

    public Instant getAgregadoEn() {
        return agregadoEn;
    }
}

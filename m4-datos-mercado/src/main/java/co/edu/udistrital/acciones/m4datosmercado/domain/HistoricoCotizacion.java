package co.edu.udistrital.acciones.m4datosmercado.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "historico_cotizacion", indexes = {
        @Index(name = "idx_historico_symbol_timestamp", columnList = "symbol, timestamp DESC"),
        @Index(name = "idx_historico_symbol_intervalo_timestamp", columnList = "symbol, intervalo, timestamp DESC")
})
public class HistoricoCotizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    @Column(name = "open", nullable = false, precision = 19, scale = 4)
    private BigDecimal open;

    @Column(name = "high", nullable = false, precision = 19, scale = 4)
    private BigDecimal high;

    @Column(name = "low", nullable = false, precision = 19, scale = 4)
    private BigDecimal low;

    @Column(name = "close", nullable = false, precision = 19, scale = 4)
    private BigDecimal close;

    @Column(name = "volume", nullable = false)
    private Long volume;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "intervalo", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private Intervalo intervalo;

    protected HistoricoCotizacion() {}

    public HistoricoCotizacion(String symbol, BigDecimal open, BigDecimal high, BigDecimal low,
                               BigDecimal close, Long volume, Instant timestamp, Intervalo intervalo) {
        this.symbol = symbol;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
        this.timestamp = timestamp;
        this.intervalo = intervalo;
    }

    public Long getId() { return id; }
    public String getSymbol() { return symbol; }
    public BigDecimal getOpen() { return open; }
    public BigDecimal getHigh() { return high; }
    public BigDecimal getLow() { return low; }
    public BigDecimal getClose() { return close; }
    public Long getVolume() { return volume; }
    public Instant getTimestamp() { return timestamp; }
    public Intervalo getIntervalo() { return intervalo; }
}
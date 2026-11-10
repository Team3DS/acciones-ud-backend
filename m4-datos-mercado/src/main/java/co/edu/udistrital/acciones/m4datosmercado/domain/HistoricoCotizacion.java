package co.edu.udistrital.acciones.m4datosmercado.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "historico_cotizacion")
public class HistoricoCotizacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal open;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal high;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal low;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal close;

    @Column(nullable = false)
    private Long volume;

    @Column(nullable = false)
    private Instant timestamp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Intervalo intervalo;

    public HistoricoCotizacion() {}

    public HistoricoCotizacion(String symbol, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, Long volume, Instant timestamp, Intervalo intervalo) {
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

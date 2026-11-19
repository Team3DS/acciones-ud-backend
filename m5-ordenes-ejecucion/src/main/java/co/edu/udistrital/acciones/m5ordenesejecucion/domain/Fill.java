package co.edu.udistrital.acciones.m5ordenesejecucion.domain;

import java.math.BigDecimal;
import java.time.Instant;

public class Fill {
    private String fillId;
    private BigDecimal precio;
    private Integer cantidad;
    private Instant timestamp;

    public Fill() {}

    public Fill(String fillId, BigDecimal precio, Integer cantidad, Instant timestamp) {
        this.fillId = fillId;
        this.precio = precio;
        this.cantidad = cantidad;
        this.timestamp = timestamp;
    }

    public String getFillId() { return fillId; }
    public void setFillId(String fillId) { this.fillId = fillId; }
    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}

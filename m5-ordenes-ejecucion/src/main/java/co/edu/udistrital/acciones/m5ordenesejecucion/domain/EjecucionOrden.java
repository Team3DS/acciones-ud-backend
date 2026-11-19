package co.edu.udistrital.acciones.m5ordenesejecucion.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class EjecucionOrden {
    private Long id;
    private Long ordenId;
    private String executionId;
    private BigDecimal precio;
    private Integer cantidad;
    private BigDecimal comision;
    private Instant ejecutadaEn;
    private List<Fill> fills;

    public EjecucionOrden() {}

    public EjecucionOrden(Long id, Long ordenId, String executionId, BigDecimal precio,
                          Integer cantidad, BigDecimal comision, Instant ejecutadaEn,
                          List<Fill> fills) {
        this.id = id;
        this.ordenId = ordenId;
        this.executionId = executionId;
        this.precio = precio;
        this.cantidad = cantidad;
        this.comision = comision;
        this.ejecutadaEn = ejecutadaEn;
        this.fills = fills;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrdenId() { return ordenId; }
    public void setOrdenId(Long ordenId) { this.ordenId = ordenId; }
    public String getExecutionId() { return executionId; }
    public void setExecutionId(String executionId) { this.executionId = executionId; }
    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    public BigDecimal getComision() { return comision; }
    public void setComision(BigDecimal comision) { this.comision = comision; }
    public Instant getEjecutadaEn() { return ejecutadaEn; }
    public void setEjecutadaEn(Instant ejecutadaEn) { this.ejecutadaEn = ejecutadaEn; }
    public List<Fill> getFills() { return fills; }
    public void setFills(List<Fill> fills) { this.fills = fills; }
}

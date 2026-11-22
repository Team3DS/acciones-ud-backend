package co.edu.udistrital.acciones.m5ordenesejecucion.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

public class EjecucionOrden {
    private Long id;
    private Long ordenId;
    private String brokerExecutionId;
    private Integer cantidad;
    private BigDecimal precioEjecucion;
    private BigDecimal comision;
    private Instant ejecutadaEn;
    private Instant registradaEn;

    public EjecucionOrden() {}

    public EjecucionOrden(Long id, Long ordenId, String brokerExecutionId, Integer cantidad,
                          BigDecimal precioEjecucion, BigDecimal comision, Instant ejecutadaEn,
                          Instant registradaEn) {
        this.id = id;
        this.ordenId = ordenId;
        this.brokerExecutionId = brokerExecutionId;
        this.cantidad = cantidad;
        this.precioEjecucion = precioEjecucion;
        this.comision = comision;
        this.ejecutadaEn = ejecutadaEn;
        this.registradaEn = registradaEn != null ? registradaEn : Instant.now();
    }

    public BigDecimal importe() {
        if (precioEjecucion == null || cantidad == null) {
            return BigDecimal.ZERO;
        }
        return precioEjecucion.multiply(BigDecimal.valueOf(cantidad)).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal neto(LadoOrden lado) {
        BigDecimal base = importe();
        BigDecimal com = comision != null ? comision : BigDecimal.ZERO;
        if (lado == LadoOrden.COMPRA) {
            return base.add(com);
        } else {
            return base.subtract(com);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrdenId() { return ordenId; }
    public void setOrdenId(Long ordenId) { this.ordenId = ordenId; }
    public String getBrokerExecutionId() { return brokerExecutionId; }
    public void setBrokerExecutionId(String brokerExecutionId) { this.brokerExecutionId = brokerExecutionId; }
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    public BigDecimal getPrecioEjecucion() { return precioEjecucion; }
    public void setPrecioEjecucion(BigDecimal precioEjecucion) { this.precioEjecucion = precioEjecucion; }
    public BigDecimal getComision() { return comision; }
    public void setComision(BigDecimal comision) { this.comision = comision; }
    public Instant getEjecutadaEn() { return ejecutadaEn; }
    public void setEjecutadaEn(Instant ejecutadaEn) { this.ejecutadaEn = ejecutadaEn; }
    public Instant getRegistradaEn() { return registradaEn; }
    public void setRegistradaEn(Instant registradaEn) { this.registradaEn = registradaEn; }
}

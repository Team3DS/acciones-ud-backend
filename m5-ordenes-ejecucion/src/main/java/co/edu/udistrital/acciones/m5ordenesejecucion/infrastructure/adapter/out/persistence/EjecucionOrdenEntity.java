package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EjecucionOrden;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ejecucion_orden")
public class EjecucionOrdenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "orden_id", nullable = false)
    private Long ordenId;

    @Column(name = "broker_execution_id", nullable = false, length = 64, unique = true)
    private String brokerExecutionId;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "precio_ejecucion", nullable = false, precision = 18, scale = 4)
    private BigDecimal precioEjecucion;

    @Column(name = "comision", nullable = false, precision = 18, scale = 2)
    private BigDecimal comision;

    @Column(name = "ejecutada_en", nullable = false)
    private Instant ejecutadaEn;

    @Column(name = "registrada_en", nullable = false)
    private Instant registradaEn;

    public EjecucionOrdenEntity() {}

    public static EjecucionOrdenEntity fromDomain(EjecucionOrden domain) {
        EjecucionOrdenEntity entity = new EjecucionOrdenEntity();
        entity.id = domain.getId();
        entity.ordenId = domain.getOrdenId();
        entity.brokerExecutionId = domain.getBrokerExecutionId();
        entity.cantidad = domain.getCantidad();
        entity.precioEjecucion = domain.getPrecioEjecucion();
        entity.comision = domain.getComision();
        entity.ejecutadaEn = domain.getEjecutadaEn();
        entity.registradaEn = domain.getRegistradaEn() != null ? domain.getRegistradaEn() : Instant.now();
        return entity;
    }

    public EjecucionOrden toDomain() {
        return new EjecucionOrden(
                this.id,
                this.ordenId,
                this.brokerExecutionId,
                this.cantidad,
                this.precioEjecucion,
                this.comision,
                this.ejecutadaEn,
                this.registradaEn
        );
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

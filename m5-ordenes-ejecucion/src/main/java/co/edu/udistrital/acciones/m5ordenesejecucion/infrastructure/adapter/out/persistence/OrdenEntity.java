package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EstadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.LadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.Orden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.TipoOrden;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orden")
public class OrdenEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long usuarioId;

    @Column(nullable = false, length = 20)
    private String simbolo;

    @Column(nullable = false, length = 30)
    private String mercado = "NYSE";

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private LadoOrden lado;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TipoOrden tipo;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal cantidad;

    @Column(precision = 19, scale = 4)
    private BigDecimal precioLimite;

    @Column(precision = 19, scale = 4)
    private BigDecimal precioEjecutado;

    @Column(precision = 19, scale = 4)
    private BigDecimal comision;

    @Column(precision = 19, scale = 4)
    private BigDecimal valorTotal;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private EstadoOrden estado;

    @Column(length = 100)
    private String clientOrderId;

    @Column(length = 100)
    private String ordenExternaId;

    @Column(length = 100)
    private String reservaFondosId;

    @Column(length = 100)
    private String reservaPosicionId;

    private Instant creadoEn;
    private Instant actualizadoEn;
    private Instant enviadaEn;
    private Instant ejecutadaEn;
    private Integer cantidadEjecutada;
    private Integer cantidadRemanente;

    public OrdenEntity() {}

    public OrdenEntity(Orden orden) {
        this.id = orden.getId();
        this.usuarioId = orden.getUsuarioId();
        this.simbolo = orden.getSimbolo();
        this.mercado = orden.getMercado();
        this.lado = orden.getLado();
        this.tipo = orden.getTipo();
        this.cantidad = orden.getCantidad();
        this.precioLimite = orden.getPrecioLimite();
        this.precioEjecutado = orden.getPrecioEjecutado();
        this.comision = orden.getComision();
        this.valorTotal = orden.getValorTotal();
        this.estado = orden.getEstado();
        this.clientOrderId = orden.getClientOrderId();
        this.ordenExternaId = orden.getOrdenExternaId();
        this.reservaFondosId = orden.getReservaFondosId();
        this.reservaPosicionId = orden.getReservaPosicionId();
        this.creadoEn = orden.getCreadoEn();
        this.actualizadoEn = orden.getActualizadoEn();
        this.enviadaEn = orden.getEnviadaEn();
        this.ejecutadaEn = orden.getEjecutadaEn();
        this.cantidadEjecutada = orden.getCantidadEjecutada();
        this.cantidadRemanente = orden.getCantidadRemanente();
    }

    public Orden toDomain() {
        Orden orden = new Orden();
        orden.setId(id);
        orden.setUsuarioId(usuarioId);
        orden.setSimbolo(simbolo);
        orden.setMercado(mercado);
        orden.setLado(lado);
        orden.setTipo(tipo);
        orden.setCantidad(cantidad);
        orden.setPrecioLimite(precioLimite);
        orden.setPrecioEjecutado(precioEjecutado);
        orden.setComision(comision);
        orden.setValorTotal(valorTotal);
        orden.setEstado(estado);
        orden.setClientOrderId(clientOrderId);
        orden.setOrdenExternaId(ordenExternaId);
        orden.setReservaFondosId(reservaFondosId);
        orden.setReservaPosicionId(reservaPosicionId);
        orden.setCreadoEn(creadoEn);
        orden.setActualizadoEn(actualizadoEn);
        orden.setEnviadaEn(enviadaEn);
        orden.setEjecutadaEn(ejecutadaEn);
        orden.setCantidadEjecutada(cantidadEjecutada);
        orden.setCantidadRemanente(cantidadRemanente);
        return orden;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public String getSimbolo() { return simbolo; }
    public void setSimbolo(String simbolo) { this.simbolo = simbolo; }
    public String getMercado() { return mercado; }
    public void setMercado(String mercado) { this.mercado = mercado; }
    public LadoOrden getLado() { return lado; }
    public void setLado(LadoOrden lado) { this.lado = lado; }
    public TipoOrden getTipo() { return tipo; }
    public void setTipo(TipoOrden tipo) { this.tipo = tipo; }
    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }
    public BigDecimal getPrecioLimite() { return precioLimite; }
    public void setPrecioLimite(BigDecimal precioLimite) { this.precioLimite = precioLimite; }
    public BigDecimal getPrecioEjecutado() { return precioEjecutado; }
    public void setPrecioEjecutado(BigDecimal precioEjecutado) { this.precioEjecutado = precioEjecutado; }
    public BigDecimal getComision() { return comision; }
    public void setComision(BigDecimal comision) { this.comision = comision; }
    public BigDecimal getValorTotal() { return valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }
    public EstadoOrden getEstado() { return estado; }
    public void setEstado(EstadoOrden estado) { this.estado = estado; }
    public String getClientOrderId() { return clientOrderId; }
    public void setClientOrderId(String clientOrderId) { this.clientOrderId = clientOrderId; }
    public String getOrdenExternaId() { return ordenExternaId; }
    public void setOrdenExternaId(String ordenExternaId) { this.ordenExternaId = ordenExternaId; }
    public String getReservaFondosId() { return reservaFondosId; }
    public void setReservaFondosId(String reservaFondosId) { this.reservaFondosId = reservaFondosId; }
    public String getReservaPosicionId() { return reservaPosicionId; }
    public void setReservaPosicionId(String reservaPosicionId) { this.reservaPosicionId = reservaPosicionId; }
    public Instant getCreadoEn() { return creadoEn; }
    public void setCreadoEn(Instant creadoEn) { this.creadoEn = creadoEn; }
    public Instant getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(Instant actualizadoEn) { this.actualizadoEn = actualizadoEn; }
    public Instant getEnviadaEn() { return enviadaEn; }
    public void setEnviadaEn(Instant enviadaEn) { this.enviadaEn = enviadaEn; }
    public Instant getEjecutadaEn() { return ejecutadaEn; }
    public void setEjecutadaEn(Instant ejecutadaEn) { this.ejecutadaEn = ejecutadaEn; }
    public Integer getCantidadEjecutada() { return cantidadEjecutada; }
    public void setCantidadEjecutada(Integer cantidadEjecutada) { this.cantidadEjecutada = cantidadEjecutada; }
    public Integer getCantidadRemanente() { return cantidadRemanente; }
    public void setCantidadRemanente(Integer cantidadRemanente) { this.cantidadRemanente = cantidadRemanente; }
}

package co.edu.udistrital.acciones.m5ordenesejecucion.domain;

import java.math.BigDecimal;
import java.time.Instant;

public class Orden {
    private Long id;
    private Long usuarioId;
    private Long accionId;
    private LadoOrden lado;
    private TipoOrden tipo;
    private BigDecimal cantidad;
    private BigDecimal precioLimite;
    private BigDecimal precioEjecutado;
    private BigDecimal comision;
    private BigDecimal valorTotal;
    private EstadoOrden estado;
    private String simbolo;
    private String mercado;
    private String reservaFondosId;
    private String reservaPosicionId;
    private String clientOrderId;
    private String ordenExternaId;
    private Instant creadoEn;
    private Instant actualizadoEn;
    private Instant enviadaEn;
    private Instant ejecutadaEn;
    private Integer cantidadEjecutada;
    private Integer cantidadRemanente;

    public Orden() {}

    public Orden(Long id, Long usuarioId, Long accionId, LadoOrden lado, TipoOrden tipo,
                 BigDecimal cantidad, BigDecimal precioLimite, BigDecimal precioEjecutado,
                 BigDecimal comision, BigDecimal valorTotal, EstadoOrden estado, String simbolo,
                 String mercado, String reservaFondosId, String reservaPosicionId,
                 String clientOrderId, String ordenExternaId, Instant creadoEn,
                 Instant actualizadoEn, Instant enviadaEn, Instant ejecutadaEn,
                 Integer cantidadEjecutada, Integer cantidadRemanente) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.accionId = accionId;
        this.lado = lado;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.precioLimite = precioLimite;
        this.precioEjecutado = precioEjecutado;
        this.comision = comision;
        this.valorTotal = valorTotal;
        this.estado = estado;
        this.simbolo = simbolo;
        this.mercado = mercado;
        this.reservaFondosId = reservaFondosId;
        this.reservaPosicionId = reservaPosicionId;
        this.clientOrderId = clientOrderId;
        this.ordenExternaId = ordenExternaId;
        this.creadoEn = creadoEn;
        this.actualizadoEn = actualizadoEn;
        this.enviadaEn = enviadaEn;
        this.ejecutadaEn = ejecutadaEn;
        this.cantidadEjecutada = cantidadEjecutada;
        this.cantidadRemanente = cantidadRemanente;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public Long getAccionId() { return accionId; }
    public void setAccionId(Long accionId) { this.accionId = accionId; }
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
    public String getSimbolo() { return simbolo; }
    public void setSimbolo(String simbolo) { this.simbolo = simbolo; }
    public String getMercado() { return mercado; }
    public void setMercado(String mercado) { this.mercado = mercado; }
    public String getReservaFondosId() { return reservaFondosId; }
    public void setReservaFondosId(String reservaFondosId) { this.reservaFondosId = reservaFondosId; }
    public String getReservaPosicionId() { return reservaPosicionId; }
    public void setReservaPosicionId(String reservaPosicionId) { this.reservaPosicionId = reservaPosicionId; }
    public String getClientOrderId() { return clientOrderId; }
    public void setClientOrderId(String clientOrderId) { this.clientOrderId = clientOrderId; }
    public String getOrdenExternaId() { return ordenExternaId; }
    public void setOrdenExternaId(String ordenExternaId) { this.ordenExternaId = ordenExternaId; }
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
    public boolean esEjecutable() {
        return estado == EstadoOrden.PENDIENTE || estado == EstadoOrden.EN_REVISION;
    }
    public boolean esCancelable() {
        return estado == EstadoOrden.PENDIENTE;
    }
}

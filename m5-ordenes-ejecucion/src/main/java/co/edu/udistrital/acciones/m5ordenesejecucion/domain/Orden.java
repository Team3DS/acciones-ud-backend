package co.edu.udistrital.acciones.m5ordenesejecucion.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class Orden {
    private Long id;
    private Long usuarioId;
    private Long accionId;
    private String ticker;
    private TipoOrden tipoOrden;
    private LadoOrden ladoOrden;
    private VigenciaOrden vigenciaOrden;
    private EstadoOrden estadoOrden;
    private Long configuracionComisionId;
    private Integer cantidad;
    private BigDecimal precioLimite;
    private BigDecimal precioActivacion;
    private BigDecimal precioEstimado;
    private Boolean requiereEncolamiento;
    private Instant confirmadoEncolamientoEn;
    private UUID clientOrderId;
    private String brokerOrderId;
    private Long reservaFondosId;
    private Long reservaPosicionId;
    private Instant expiraEn;
    private Long version;
    private Instant creadaEn;
    private Instant actualizadaEn;

    public Orden() {
        this.requiereEncolamiento = false;
        this.version = 0L;
    }

    public Orden(Long id, Long usuarioId, Long accionId, String ticker, TipoOrden tipoOrden,
                 LadoOrden ladoOrden, VigenciaOrden vigenciaOrden, EstadoOrden estadoOrden,
                 Long configuracionComisionId, Integer cantidad, BigDecimal precioLimite,
                 BigDecimal precioActivacion, BigDecimal precioEstimado, Boolean requiereEncolamiento,
                 Instant confirmadoEncolamientoEn, UUID clientOrderId, String brokerOrderId,
                 Long reservaFondosId, Long reservaPosicionId, Instant expiraEn, Long version,
                 Instant creadaEn, Instant actualizadaEn) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.accionId = accionId;
        this.ticker = ticker;
        this.tipoOrden = tipoOrden;
        this.ladoOrden = ladoOrden;
        this.vigenciaOrden = vigenciaOrden;
        this.estadoOrden = estadoOrden;
        this.configuracionComisionId = configuracionComisionId;
        this.cantidad = cantidad;
        this.precioLimite = precioLimite;
        this.precioActivacion = precioActivacion;
        this.precioEstimado = precioEstimado;
        this.requiereEncolamiento = requiereEncolamiento != null ? requiereEncolamiento : false;
        this.confirmadoEncolamientoEn = confirmadoEncolamientoEn;
        this.clientOrderId = clientOrderId;
        this.brokerOrderId = brokerOrderId;
        this.reservaFondosId = reservaFondosId;
        this.reservaPosicionId = reservaPosicionId;
        this.expiraEn = expiraEn;
        this.version = version != null ? version : 0L;
        this.creadaEn = creadaEn != null ? creadaEn : Instant.now();
        this.actualizadaEn = actualizadaEn != null ? actualizadaEn : Instant.now();
    }

    public void registrarEstado(EstadoOrden nuevoEstado, TipoActor actor, Long actorId, String causa) {
        this.estadoOrden = nuevoEstado;
        this.actualizadaEn = Instant.now();
    }

    public Integer cantidadEjecutada(List<EjecucionOrden> fills) {
        if (fills == null || fills.isEmpty()) {
            return 0;
        }
        return fills.stream()
                .mapToInt(EjecucionOrden::getCantidad)
                .sum();
    }

    public boolean esCancelable() {
        return estadoOrden == EstadoOrden.PENDIENTE || estadoOrden == EstadoOrden.EN_REVISION;
    }

    public boolean esEjecutable() {
        return estadoOrden == EstadoOrden.ENVIADA
                || estadoOrden == EstadoOrden.EN_EJECUCION
                || estadoOrden == EstadoOrden.PARCIALMENTE_EJECUTADA
                || estadoOrden == EstadoOrden.PENDIENTE;
    }

    public void confirmarEncolamiento() {
        this.confirmadoEncolamientoEn = Instant.now();
        this.actualizadaEn = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public Long getAccionId() { return accionId; }
    public void setAccionId(Long accionId) { this.accionId = accionId; }
    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }
    public TipoOrden getTipoOrden() { return tipoOrden; }
    public void setTipoOrden(TipoOrden tipoOrden) { this.tipoOrden = tipoOrden; }
    public LadoOrden getLadoOrden() { return ladoOrden; }
    public void setLadoOrden(LadoOrden ladoOrden) { this.ladoOrden = ladoOrden; }
    public VigenciaOrden getVigenciaOrden() { return vigenciaOrden; }
    public void setVigenciaOrden(VigenciaOrden vigenciaOrden) { this.vigenciaOrden = vigenciaOrden; }
    public EstadoOrden getEstadoOrden() { return estadoOrden; }
    public void setEstadoOrden(EstadoOrden estadoOrden) { this.estadoOrden = estadoOrden; }
    public Long getConfiguracionComisionId() { return configuracionComisionId; }
    public void setConfiguracionComisionId(Long configuracionComisionId) { this.configuracionComisionId = configuracionComisionId; }
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    public BigDecimal getPrecioLimite() { return precioLimite; }
    public void setPrecioLimite(BigDecimal precioLimite) { this.precioLimite = precioLimite; }
    public BigDecimal getPrecioActivacion() { return precioActivacion; }
    public void setPrecioActivacion(BigDecimal precioActivacion) { this.precioActivacion = precioActivacion; }
    public BigDecimal getPrecioEstimado() { return precioEstimado; }
    public void setPrecioEstimado(BigDecimal precioEstimado) { this.precioEstimado = precioEstimado; }
    public Boolean getRequiereEncolamiento() { return requiereEncolamiento; }
    public void setRequiereEncolamiento(Boolean requiereEncolamiento) { this.requiereEncolamiento = requiereEncolamiento; }
    public Instant getConfirmadoEncolamientoEn() { return confirmadoEncolamientoEn; }
    public void setConfirmadoEncolamientoEn(Instant confirmadoEncolamientoEn) { this.confirmadoEncolamientoEn = confirmadoEncolamientoEn; }
    public UUID getClientOrderId() { return clientOrderId; }
    public void setClientOrderId(UUID clientOrderId) { this.clientOrderId = clientOrderId; }
    public String getBrokerOrderId() { return brokerOrderId; }
    public void setBrokerOrderId(String brokerOrderId) { this.brokerOrderId = brokerOrderId; }
    public Long getReservaFondosId() { return reservaFondosId; }
    public void setReservaFondosId(Long reservaFondosId) { this.reservaFondosId = reservaFondosId; }
    public Long getReservaPosicionId() { return reservaPosicionId; }
    public void setReservaPosicionId(Long reservaPosicionId) { this.reservaPosicionId = reservaPosicionId; }
    public Instant getExpiraEn() { return expiraEn; }
    public void setExpiraEn(Instant expiraEn) { this.expiraEn = expiraEn; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public Instant getCreadaEn() { return creadaEn; }
    public void setCreadaEn(Instant creadaEn) { this.creadaEn = creadaEn; }
    public Instant getActualizadaEn() { return actualizadaEn; }
    public void setActualizadaEn(Instant actualizadaEn) { this.actualizadaEn = actualizadaEn; }
}

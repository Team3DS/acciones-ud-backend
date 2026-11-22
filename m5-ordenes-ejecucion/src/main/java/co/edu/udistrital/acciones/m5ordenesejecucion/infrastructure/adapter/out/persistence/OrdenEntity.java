package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EstadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.LadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.Orden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.TipoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.VigenciaOrden;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orden")
public class OrdenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "accion_id", nullable = false)
    private Long accionId;

    @Column(name = "ticker", nullable = false, length = 10)
    private String ticker;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_orden", nullable = false, length = 30)
    private TipoOrden tipoOrden;

    @Enumerated(EnumType.STRING)
    @Column(name = "lado_orden", nullable = false, length = 30)
    private LadoOrden ladoOrden;

    @Enumerated(EnumType.STRING)
    @Column(name = "vigencia_orden", nullable = false, length = 30)
    private VigenciaOrden vigenciaOrden;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_orden", nullable = false, length = 30)
    private EstadoOrden estadoOrden;

    @Column(name = "configuracion_comision_id", nullable = false)
    private Long configuracionComisionId;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "precio_limite", precision = 18, scale = 4)
    private BigDecimal precioLimite;

    @Column(name = "precio_activacion", precision = 18, scale = 4)
    private BigDecimal precioActivacion;

    @Column(name = "precio_estimado", nullable = false, precision = 18, scale = 4)
    private BigDecimal precioEstimado;

    @Column(name = "requiere_encolamiento", nullable = false)
    private Boolean requiereEncolamiento = false;

    @Column(name = "confirmado_encolamiento_en")
    private Instant confirmadoEncolamientoEn;

    @Column(name = "client_order_id", nullable = false, unique = true)
    private UUID clientOrderId;

    @Column(name = "broker_order_id", length = 64, unique = true)
    private String brokerOrderId;

    @Column(name = "reserva_fondos_id", unique = true)
    private Long reservaFondosId;

    @Column(name = "reserva_posicion_id", unique = true)
    private Long reservaPosicionId;

    @Column(name = "expira_en")
    private Instant expiraEn;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Column(name = "creada_en", nullable = false)
    private Instant creadaEn;

    @Column(name = "actualizada_en", nullable = false)
    private Instant actualizadaEn;

    public OrdenEntity() {}

    public static OrdenEntity fromDomain(Orden orden) {
        OrdenEntity entity = new OrdenEntity();
        entity.id = orden.getId();
        entity.usuarioId = orden.getUsuarioId();
        entity.accionId = orden.getAccionId();
        entity.ticker = orden.getTicker();
        entity.tipoOrden = orden.getTipoOrden();
        entity.ladoOrden = orden.getLadoOrden();
        entity.vigenciaOrden = orden.getVigenciaOrden();
        entity.estadoOrden = orden.getEstadoOrden();
        entity.configuracionComisionId = orden.getConfiguracionComisionId();
        entity.cantidad = orden.getCantidad();
        entity.precioLimite = orden.getPrecioLimite();
        entity.precioActivacion = orden.getPrecioActivacion();
        entity.precioEstimado = orden.getPrecioEstimado();
        entity.requiereEncolamiento = orden.getRequiereEncolamiento() != null ? orden.getRequiereEncolamiento() : false;
        entity.confirmadoEncolamientoEn = orden.getConfirmadoEncolamientoEn();
        entity.clientOrderId = orden.getClientOrderId();
        entity.brokerOrderId = orden.getBrokerOrderId();
        entity.reservaFondosId = orden.getReservaFondosId();
        entity.reservaPosicionId = orden.getReservaPosicionId();
        entity.expiraEn = orden.getExpiraEn();
        entity.version = orden.getVersion() != null ? orden.getVersion() : 0L;
        entity.creadaEn = orden.getCreadaEn() != null ? orden.getCreadaEn() : Instant.now();
        entity.actualizadaEn = orden.getActualizadaEn() != null ? orden.getActualizadaEn() : Instant.now();
        return entity;
    }

    public Orden toDomain() {
        return new Orden(
                this.id,
                this.usuarioId,
                this.accionId,
                this.ticker,
                this.tipoOrden,
                this.ladoOrden,
                this.vigenciaOrden,
                this.estadoOrden,
                this.configuracionComisionId,
                this.cantidad,
                this.precioLimite,
                this.precioActivacion,
                this.precioEstimado,
                this.requiereEncolamiento,
                this.confirmadoEncolamientoEn,
                this.clientOrderId,
                this.brokerOrderId,
                this.reservaFondosId,
                this.reservaPosicionId,
                this.expiraEn,
                this.version,
                this.creadaEn,
                this.actualizadaEn
        );
    }

    // Acceso a columnas (Hibernate usa acceso por campo; la conversión con el dominio está en fromDomain/toDomain).
    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public Long getAccionId() { return accionId; }
    public String getTicker() { return ticker; }
    public TipoOrden getTipoOrden() { return tipoOrden; }
    public LadoOrden getLadoOrden() { return ladoOrden; }
    public VigenciaOrden getVigenciaOrden() { return vigenciaOrden; }
    public EstadoOrden getEstadoOrden() { return estadoOrden; }
    public Long getConfiguracionComisionId() { return configuracionComisionId; }
    public Integer getCantidad() { return cantidad; }
    public BigDecimal getPrecioLimite() { return precioLimite; }
    public BigDecimal getPrecioActivacion() { return precioActivacion; }
    public BigDecimal getPrecioEstimado() { return precioEstimado; }
    public Boolean getRequiereEncolamiento() { return requiereEncolamiento; }
    public Instant getConfirmadoEncolamientoEn() { return confirmadoEncolamientoEn; }
    public UUID getClientOrderId() { return clientOrderId; }
    public String getBrokerOrderId() { return brokerOrderId; }
    public Long getReservaFondosId() { return reservaFondosId; }
    public Long getReservaPosicionId() { return reservaPosicionId; }
    public Instant getExpiraEn() { return expiraEn; }
    public Long getVersion() { return version; }
    public Instant getCreadaEn() { return creadaEn; }
    public Instant getActualizadaEn() { return actualizadaEn; }

    public void setId(Long id) { this.id = id; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public void setAccionId(Long accionId) { this.accionId = accionId; }
    public void setTicker(String ticker) { this.ticker = ticker; }
    public void setTipoOrden(TipoOrden tipoOrden) { this.tipoOrden = tipoOrden; }
    public void setLadoOrden(LadoOrden ladoOrden) { this.ladoOrden = ladoOrden; }
    public void setVigenciaOrden(VigenciaOrden vigenciaOrden) { this.vigenciaOrden = vigenciaOrden; }
    public void setEstadoOrden(EstadoOrden estadoOrden) { this.estadoOrden = estadoOrden; }
    public void setConfiguracionComisionId(Long configuracionComisionId) { this.configuracionComisionId = configuracionComisionId; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    public void setPrecioLimite(BigDecimal precioLimite) { this.precioLimite = precioLimite; }
    public void setPrecioActivacion(BigDecimal precioActivacion) { this.precioActivacion = precioActivacion; }
    public void setPrecioEstimado(BigDecimal precioEstimado) { this.precioEstimado = precioEstimado; }
    public void setRequiereEncolamiento(Boolean requiereEncolamiento) { this.requiereEncolamiento = requiereEncolamiento; }
    public void setConfirmadoEncolamientoEn(Instant confirmadoEncolamientoEn) { this.confirmadoEncolamientoEn = confirmadoEncolamientoEn; }
    public void setClientOrderId(UUID clientOrderId) { this.clientOrderId = clientOrderId; }
    public void setBrokerOrderId(String brokerOrderId) { this.brokerOrderId = brokerOrderId; }
    public void setReservaFondosId(Long reservaFondosId) { this.reservaFondosId = reservaFondosId; }
    public void setReservaPosicionId(Long reservaPosicionId) { this.reservaPosicionId = reservaPosicionId; }
    public void setExpiraEn(Instant expiraEn) { this.expiraEn = expiraEn; }
    public void setVersion(Long version) { this.version = version; }
    public void setCreadaEn(Instant creadaEn) { this.creadaEn = creadaEn; }
    public void setActualizadaEn(Instant actualizadaEn) { this.actualizadaEn = actualizadaEn; }
}

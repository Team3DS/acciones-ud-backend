package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m5ordenesejecucion.domain.ConfiguracionComision;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "configuracion_comision")
public class ConfiguracionComisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mercado_codigo", nullable = false, length = 30)
    private String mercadoCodigo;

    @Column(name = "porcentaje", nullable = false, precision = 7, scale = 4)
    private BigDecimal porcentaje;

    @Column(name = "vigente_desde", nullable = false)
    private Instant vigenteDesde;

    @Column(name = "vigente_hasta")
    private Instant vigenteHasta;

    @Column(name = "motivo", nullable = false, length = 500)
    private String motivo;

    @Column(name = "creado_por_usuario_id")
    private Long creadoPorUsuarioId;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    public ConfiguracionComisionEntity() {}

    public static ConfiguracionComisionEntity fromDomain(ConfiguracionComision domain) {
        ConfiguracionComisionEntity entity = new ConfiguracionComisionEntity();
        entity.id = domain.getId();
        entity.mercadoCodigo = domain.getMercadoCodigo();
        entity.porcentaje = domain.getPorcentaje();
        entity.vigenteDesde = domain.getVigenteDesde();
        entity.vigenteHasta = domain.getVigenteHasta();
        entity.motivo = domain.getMotivo();
        entity.creadoPorUsuarioId = domain.getCreadoPorUsuarioId();
        entity.creadoEn = domain.getCreadoEn() != null ? domain.getCreadoEn() : Instant.now();
        return entity;
    }

    public ConfiguracionComision toDomain() {
        return new ConfiguracionComision(
                this.id,
                this.mercadoCodigo,
                this.porcentaje,
                this.vigenteDesde,
                this.vigenteHasta,
                this.motivo,
                this.creadoPorUsuarioId,
                this.creadoEn
        );
    }

    // Acceso a columnas (Hibernate usa acceso por campo; la conversión con el dominio está en fromDomain/toDomain).
    public Long getId() { return id; }
    public String getMercadoCodigo() { return mercadoCodigo; }
    public BigDecimal getPorcentaje() { return porcentaje; }
    public Instant getVigenteDesde() { return vigenteDesde; }
    public Instant getVigenteHasta() { return vigenteHasta; }
    public String getMotivo() { return motivo; }
    public Long getCreadoPorUsuarioId() { return creadoPorUsuarioId; }
    public Instant getCreadoEn() { return creadoEn; }

    public void setId(Long id) { this.id = id; }
    public void setMercadoCodigo(String mercadoCodigo) { this.mercadoCodigo = mercadoCodigo; }
    public void setPorcentaje(BigDecimal porcentaje) { this.porcentaje = porcentaje; }
    public void setVigenteDesde(Instant vigenteDesde) { this.vigenteDesde = vigenteDesde; }
    public void setVigenteHasta(Instant vigenteHasta) { this.vigenteHasta = vigenteHasta; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public void setCreadoPorUsuarioId(Long creadoPorUsuarioId) { this.creadoPorUsuarioId = creadoPorUsuarioId; }
    public void setCreadoEn(Instant creadoEn) { this.creadoEn = creadoEn; }
}

package co.edu.udistrital.acciones.m5ordenesejecucion.domain;

import java.math.BigDecimal;
import java.time.Instant;

public class ConfiguracionComision {
    private Long id;
    private String mercadoCodigo;
    private BigDecimal porcentaje;
    private Instant vigenteDesde;
    private Instant vigenteHasta;
    private String motivo;
    private Long creadoPorUsuarioId;
    private Instant creadoEn;

    public ConfiguracionComision() {}

    public ConfiguracionComision(Long id, String mercadoCodigo, BigDecimal porcentaje,
                                 Instant vigenteDesde, Instant vigenteHasta, String motivo,
                                 Long creadoPorUsuarioId, Instant creadoEn) {
        this.id = id;
        this.mercadoCodigo = mercadoCodigo;
        this.porcentaje = porcentaje;
        this.vigenteDesde = vigenteDesde;
        this.vigenteHasta = vigenteHasta;
        this.motivo = motivo;
        this.creadoPorUsuarioId = creadoPorUsuarioId;
        this.creadoEn = creadoEn;
    }

    public boolean estaVigente(Instant momento) {
        if (momento == null) {
            momento = Instant.now();
        }
        boolean despuesDeInicio = vigenteDesde != null && !momento.isBefore(vigenteDesde);
        boolean antesDeFin = vigenteHasta == null || momento.isBefore(vigenteHasta);
        return despuesDeInicio && antesDeFin;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMercadoCodigo() { return mercadoCodigo; }
    public void setMercadoCodigo(String mercadoCodigo) { this.mercadoCodigo = mercadoCodigo; }
    public BigDecimal getPorcentaje() { return porcentaje; }
    public void setPorcentaje(BigDecimal porcentaje) { this.porcentaje = porcentaje; }
    public Instant getVigenteDesde() { return vigenteDesde; }
    public void setVigenteDesde(Instant vigenteDesde) { this.vigenteDesde = vigenteDesde; }
    public Instant getVigenteHasta() { return vigenteHasta; }
    public void setVigenteHasta(Instant vigenteHasta) { this.vigenteHasta = vigenteHasta; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public Long getCreadoPorUsuarioId() { return creadoPorUsuarioId; }
    public void setCreadoPorUsuarioId(Long creadoPorUsuarioId) { this.creadoPorUsuarioId = creadoPorUsuarioId; }
    public Instant getCreadoEn() { return creadoEn; }
    public void setCreadoEn(Instant creadoEn) { this.creadoEn = creadoEn; }
}

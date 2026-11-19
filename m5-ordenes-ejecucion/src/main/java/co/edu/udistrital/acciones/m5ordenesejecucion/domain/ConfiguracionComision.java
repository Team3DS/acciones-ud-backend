package co.edu.udistrital.acciones.m5ordenesejecucion.domain;

import java.math.BigDecimal;
import java.time.Instant;

public class ConfiguracionComision {
    private Long id;
    private String mercado;
    private BigDecimal porcentaje;
    private Instant vigenciaDesde;
    private Instant vigenciaHasta;
    private String creadoPor;

    public ConfiguracionComision() {}

    public ConfiguracionComision(Long id, String mercado, BigDecimal porcentaje,
                                 Instant vigenciaDesde, Instant vigenciaHasta, String creadoPor) {
        this.id = id;
        this.mercado = mercado;
        this.porcentaje = porcentaje;
        this.vigenciaDesde = vigenciaDesde;
        this.vigenciaHasta = vigenciaHasta;
        this.creadoPor = creadoPor;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMercado() { return mercado; }
    public void setMercado(String mercado) { this.mercado = mercado; }
    public BigDecimal getPorcentaje() { return porcentaje; }
    public void setPorcentaje(BigDecimal porcentaje) { this.porcentaje = porcentaje; }
    public Instant getVigenciaDesde() { return vigenciaDesde; }
    public void setVigenciaDesde(Instant vigenciaDesde) { this.vigenciaDesde = vigenciaDesde; }
    public Instant getVigenciaHasta() { return vigenciaHasta; }
    public void setVigenciaHasta(Instant vigenciaHasta) { this.vigenciaHasta = vigenciaHasta; }
    public String getCreadoPor() { return creadoPor; }
    public void setCreadoPor(String creadoPor) { this.creadoPor = creadoPor; }
}

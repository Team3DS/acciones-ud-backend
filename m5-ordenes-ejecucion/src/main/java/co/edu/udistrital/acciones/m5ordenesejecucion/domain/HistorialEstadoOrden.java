package co.edu.udistrital.acciones.m5ordenesejecucion.domain;

import java.time.Instant;

public class HistorialEstadoOrden {
    private Long id;
    private Long ordenId;
    private EstadoOrden estadoAnterior;
    private EstadoOrden estadoNuevo;
    private String motivo;
    private Instant cambiadoEn;
    private String cambiadoPor;

    public HistorialEstadoOrden() {}

    public HistorialEstadoOrden(Long id, Long ordenId, EstadoOrden estadoAnterior,
                                EstadoOrden estadoNuevo, String motivo, Instant cambiadoEn,
                                String cambiadoPor) {
        this.id = id;
        this.ordenId = ordenId;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.motivo = motivo;
        this.cambiadoEn = cambiadoEn;
        this.cambiadoPor = cambiadoPor;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrdenId() { return ordenId; }
    public void setOrdenId(Long ordenId) { this.ordenId = ordenId; }
    public EstadoOrden getEstadoAnterior() { return estadoAnterior; }
    public void setEstadoAnterior(EstadoOrden estadoAnterior) { this.estadoAnterior = estadoAnterior; }
    public EstadoOrden getEstadoNuevo() { return estadoNuevo; }
    public void setEstadoNuevo(EstadoOrden estadoNuevo) { this.estadoNuevo = estadoNuevo; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public Instant getCambiadoEn() { return cambiadoEn; }
    public void setCambiadoEn(Instant cambiadoEn) { this.cambiadoEn = cambiadoEn; }
    public String getCambiadoPor() { return cambiadoPor; }
    public void setCambiadoPor(String cambiadoPor) { this.cambiadoPor = cambiadoPor; }
}

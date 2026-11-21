package co.edu.udistrital.acciones.m5ordenesejecucion.domain;

import java.time.Instant;

public class HistorialEstadoOrden {
    private Long id;
    private Long ordenId;
    private EstadoOrden estadoOrden;
    private TipoActor tipoActor;
    private Long actorUsuarioId;
    private String causa;
    private Instant ocurridoEn;

    public HistorialEstadoOrden() {}

    public HistorialEstadoOrden(Long id, Long ordenId, EstadoOrden estadoOrden,
                                TipoActor tipoActor, Long actorUsuarioId, String causa,
                                Instant ocurridoEn) {
        this.id = id;
        this.ordenId = ordenId;
        this.estadoOrden = estadoOrden;
        this.tipoActor = tipoActor;
        this.actorUsuarioId = actorUsuarioId;
        this.causa = causa;
        this.ocurridoEn = ocurridoEn;
    }

    public static HistorialEstadoOrden registrar(Long ordenId, EstadoOrden estado,
                                                TipoActor actor, Long actorId, String causa) {
        return new HistorialEstadoOrden(null, ordenId, estado, actor, actorId, causa, Instant.now());
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrdenId() { return ordenId; }
    public void setOrdenId(Long ordenId) { this.ordenId = ordenId; }
    public EstadoOrden getEstadoOrden() { return estadoOrden; }
    public void setEstadoOrden(EstadoOrden estadoOrden) { this.estadoOrden = estadoOrden; }
    public TipoActor getTipoActor() { return tipoActor; }
    public void setTipoActor(TipoActor tipoActor) { this.tipoActor = tipoActor; }
    public Long getActorUsuarioId() { return actorUsuarioId; }
    public void setActorUsuarioId(Long actorUsuarioId) { this.actorUsuarioId = actorUsuarioId; }
    public String getCausa() { return causa; }
    public void setCausa(String causa) { this.causa = causa; }
    public Instant getOcurridoEn() { return ocurridoEn; }
    public void setOcurridoEn(Instant ocurridoEn) { this.ocurridoEn = ocurridoEn; }
}

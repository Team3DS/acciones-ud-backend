package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EstadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.HistorialEstadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.TipoActor;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "historial_estado_orden")
public class HistorialEstadoOrdenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "orden_id", nullable = false)
    private Long ordenId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_orden", nullable = false, length = 30)
    private EstadoOrden estadoOrden;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_actor", nullable = false, length = 30)
    private TipoActor tipoActor;

    @Column(name = "actor_usuario_id")
    private Long actorUsuarioId;

    @Column(name = "causa", nullable = false, length = 500)
    private String causa;

    @Column(name = "ocurrido_en", nullable = false)
    private Instant ocurridoEn;

    public HistorialEstadoOrdenEntity() {}

    public static HistorialEstadoOrdenEntity fromDomain(HistorialEstadoOrden domain) {
        HistorialEstadoOrdenEntity entity = new HistorialEstadoOrdenEntity();
        entity.id = domain.getId();
        entity.ordenId = domain.getOrdenId();
        entity.estadoOrden = domain.getEstadoOrden();
        entity.tipoActor = domain.getTipoActor();
        entity.actorUsuarioId = domain.getActorUsuarioId();
        entity.causa = domain.getCausa();
        entity.ocurridoEn = domain.getOcurridoEn() != null ? domain.getOcurridoEn() : Instant.now();
        return entity;
    }

    public HistorialEstadoOrden toDomain() {
        return new HistorialEstadoOrden(
                this.id,
                this.ordenId,
                this.estadoOrden,
                this.tipoActor,
                this.actorUsuarioId,
                this.causa,
                this.ocurridoEn
        );
    }

    // Acceso a columnas (Hibernate usa acceso por campo; la conversión con el dominio está en fromDomain/toDomain).
    public Long getId() { return id; }
    public Long getOrdenId() { return ordenId; }
    public EstadoOrden getEstadoOrden() { return estadoOrden; }
    public TipoActor getTipoActor() { return tipoActor; }
    public Long getActorUsuarioId() { return actorUsuarioId; }
    public String getCausa() { return causa; }
    public Instant getOcurridoEn() { return ocurridoEn; }

    public void setId(Long id) { this.id = id; }
    public void setOrdenId(Long ordenId) { this.ordenId = ordenId; }
    public void setEstadoOrden(EstadoOrden estadoOrden) { this.estadoOrden = estadoOrden; }
    public void setTipoActor(TipoActor tipoActor) { this.tipoActor = tipoActor; }
    public void setActorUsuarioId(Long actorUsuarioId) { this.actorUsuarioId = actorUsuarioId; }
    public void setCausa(String causa) { this.causa = causa; }
    public void setOcurridoEn(Instant ocurridoEn) { this.ocurridoEn = ocurridoEn; }
}

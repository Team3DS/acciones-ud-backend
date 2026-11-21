package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m5ordenesejecucion.domain.OutboxEvento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_evento")
public class OutboxEventoEntity {

    @Id
    private UUID id;

    @Column(name = "tipo_agregado", nullable = false, length = 50)
    private String tipoAgregado;

    @Column(name = "agregado_id", nullable = false, length = 64)
    private String agregadoId;

    @Column(name = "tipo_evento", nullable = false, length = 100)
    private String tipoEvento;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "carga", nullable = false)
    private String carga;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "publicado_en")
    private Instant publicadoEn;

    @Column(name = "intentos", nullable = false)
    private Integer intentos = 0;

    @Column(name = "ultimo_error", length = 500)
    private String ultimoError;

    public OutboxEventoEntity() {}

    public static OutboxEventoEntity fromDomain(OutboxEvento domain) {
        OutboxEventoEntity entity = new OutboxEventoEntity();
        entity.id = domain.getId();
        entity.tipoAgregado = domain.getTipoAgregado();
        entity.agregadoId = domain.getAgregadoId();
        entity.tipoEvento = domain.getTipoEvento();
        entity.carga = domain.getCarga();
        entity.creadoEn = domain.getCreadoEn() != null ? domain.getCreadoEn() : Instant.now();
        entity.publicadoEn = domain.getPublicadoEn();
        entity.intentos = domain.getIntentos() != null ? domain.getIntentos() : 0;
        entity.ultimoError = domain.getUltimoError();
        return entity;
    }

    public OutboxEvento toDomain() {
        return new OutboxEvento(
                this.id,
                this.tipoAgregado,
                this.agregadoId,
                this.tipoEvento,
                this.carga,
                this.creadoEn,
                this.publicadoEn,
                this.intentos,
                this.ultimoError
        );
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getTipoAgregado() { return tipoAgregado; }
    public void setTipoAgregado(String tipoAgregado) { this.tipoAgregado = tipoAgregado; }
    public String getAgregadoId() { return agregadoId; }
    public void setAgregadoId(String agregadoId) { this.agregadoId = agregadoId; }
    public String getTipoEvento() { return tipoEvento; }
    public void setTipoEvento(String tipoEvento) { this.tipoEvento = tipoEvento; }
    public String getCarga() { return carga; }
    public void setCarga(String carga) { this.carga = carga; }
    public Instant getCreadoEn() { return creadoEn; }
    public void setCreadoEn(Instant creadoEn) { this.creadoEn = creadoEn; }
    public Instant getPublicadoEn() { return publicadoEn; }
    public void setPublicadoEn(Instant publicadoEn) { this.publicadoEn = publicadoEn; }
    public Integer getIntentos() { return intentos; }
    public void setIntentos(Integer intentos) { this.intentos = intentos; }
    public String getUltimoError() { return ultimoError; }
    public void setUltimoError(String ultimoError) { this.ultimoError = ultimoError; }
}

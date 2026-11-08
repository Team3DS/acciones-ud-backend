package co.edu.udistrital.acciones.m1identidad.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_evento")
public class OutboxEvento {
    @Id
    private UUID id;
    private String tipoAgregado;
    private String agregadoId;
    private String tipoEvento;
    @Column(columnDefinition = "jsonb")
    private String carga;
    private OffsetDateTime creadoEn;
    private OffsetDateTime publicadoEn;
    private Integer intentos;
    private String ultimoError;

    protected OutboxEvento() {}
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
}

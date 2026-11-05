package co.edu.udistrital.acciones.m1identidad.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "usuario_sector_interes")
public class UsuarioSectorInteres {
    @EmbeddedId
    private UsuarioSectorInteresId id;

    @Column(nullable = false)
    private OffsetDateTime creadoEn;

    protected UsuarioSectorInteres() {}

    public UsuarioSectorInteresId getId() { return id; }
    public void setId(UsuarioSectorInteresId id) { this.id = id; }
    public OffsetDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(OffsetDateTime creadoEn) { this.creadoEn = creadoEn; }
}
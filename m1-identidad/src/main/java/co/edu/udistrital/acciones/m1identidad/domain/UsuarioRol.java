package co.edu.udistrital.acciones.m1identidad.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "usuario_rol")
public class UsuarioRol {
    @EmbeddedId
    private UsuarioRolId id;

    @Column(nullable = false)
    private OffsetDateTime asignadoEn;

    protected UsuarioRol() {}

    public UsuarioRolId getId() { return id; }
    public void setId(UsuarioRolId id) { this.id = id; }
    public OffsetDateTime getAsignadoEn() { return asignadoEn; }
    public void setAsignadoEn(OffsetDateTime asignadoEn) { this.asignadoEn = asignadoEn; }
}
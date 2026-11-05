package co.edu.udistrital.acciones.m1identidad.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "USUARIO_ROL")
public class UsuarioRol {
    @EmbeddedId
    private UsuarioRolId id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Rol rol;

    @Column(nullable = false)
    private OffsetDateTime asignadoEn;

    protected UsuarioRol() {}

    public UsuarioRolId getId() { return id; }
    public void setId(UsuarioRolId id) { this.id = id; }
    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }
    public OffsetDateTime getAsignadoEn() { return asignadoEn; }
    public void setAsignadoEn(OffsetDateTime asignadoEn) { this.asignadoEn = asignadoEn; }
}
package co.edu.udistrital.acciones.m1identidad.domain;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class UsuarioRolId implements Serializable {
    private Long usuarioId;
    private String rol;

    public UsuarioRolId() {}
    public UsuarioRolId(Long usuarioId, String rol) { this.usuarioId = usuarioId; this.rol = rol; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    @Override public boolean equals(Object o) { return o instanceof UsuarioRolId that && Objects.equals(usuarioId, that.usuarioId) && Objects.equals(rol, that.rol); }
    @Override public int hashCode() { return Objects.hash(usuarioId, rol); }
}

package co.edu.udistrital.acciones.m1identidad.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class UsuarioSectorInteresId implements Serializable {
    private Long usuarioId;
    private String sector;

    public UsuarioSectorInteresId() {}
    public UsuarioSectorInteresId(Long usuarioId, String sector) {
        this.usuarioId = usuarioId;
        this.sector = sector;
    }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }

    @Override public boolean equals(Object o) {
        return o instanceof UsuarioSectorInteresId that
            && Objects.equals(usuarioId, that.usuarioId)
            && Objects.equals(sector, that.sector);
    }
    @Override public int hashCode() { return Objects.hash(usuarioId, sector); }
}
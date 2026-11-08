package co.edu.udistrital.acciones.m1identidad.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class UsuarioMercadoInteresId implements Serializable {
    private Long usuarioId;
    private String mercado;

    public UsuarioMercadoInteresId() {}
    public UsuarioMercadoInteresId(Long usuarioId, String mercado) {
        this.usuarioId = usuarioId;
        this.mercado = mercado;
    }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public String getMercado() { return mercado; }
    public void setMercado(String mercado) { this.mercado = mercado; }

    @Override public boolean equals(Object o) {
        return o instanceof UsuarioMercadoInteresId that
            && Objects.equals(usuarioId, that.usuarioId)
            && Objects.equals(mercado, that.mercado);
    }
    @Override public int hashCode() { return Objects.hash(usuarioId, mercado); }
}
package co.edu.udistrital.acciones.m1identidad.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "USUARIO_MERCADO_INTERES")
public class UsuarioMercadoInteres {
    @EmbeddedId
    private UsuarioMercadoInteresId id;

    @Column(nullable = false)
    private OffsetDateTime creadoEn;

    protected UsuarioMercadoInteres() {}

    public UsuarioMercadoInteresId getId() { return id; }
    public void setId(UsuarioMercadoInteresId id) { this.id = id; }
    public OffsetDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(OffsetDateTime creadoEn) { this.creadoEn = creadoEn; }
}
package co.edu.udistrital.acciones.m1identidad.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "historial_estado_usuario")
public class HistorialEstadoUsuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long usuarioId;
    private String estadoUsuario;
    private Long cambiadoPorUsuarioId;
    @Column(length = 500)
    private String motivo;
    private OffsetDateTime cambiadoEn;

    protected HistorialEstadoUsuario() {}
    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public String getEstadoUsuario() { return estadoUsuario; }
    public void setEstadoUsuario(String estadoUsuario) { this.estadoUsuario = estadoUsuario; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}

package co.edu.udistrital.acciones.m1identidad.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "USUARIO")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombres;

    @Column(nullable = false, length = 100)
    private String apellidos;

    @Column(nullable = false, length = 30)
    private String tipoDocumento;

    @Column(nullable = false)
    private byte[] numeroDocumentoCifrado;

    @Column(nullable = false)
    private byte[] numeroDocumentoHash;

    @Column(nullable = false, length = 254, unique = true)
    private String correo;

    private OffsetDateTime correoVerificadoEn;

    @Column(nullable = false)
    private byte[] celularCifrado;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 30)
    private String nivelInversionista;

    @Column(nullable = false, length = 30)
    private String estadoUsuario = "PENDIENTE_VERIFICACION";

    @Column(nullable = false)
    private Boolean solicitaReporteMensual = false;

    @Column(nullable = false)
    private Boolean aceptoPoliticaDatos = true;

    @Column(nullable = false)
    private OffsetDateTime politicaAceptadaEn;

    private String ipRegistro;

    @Column(nullable = false)
    private OffsetDateTime creadoEn;

    private OffsetDateTime actualizadoEn;

    protected User() {}

    public Long getId() { return id; }
    public String getCorreo() { return correo; }
    public String getEstadoUsuario() { return estadoUsuario; }

    public void setNombres(String nombres) { this.nombres = nombres; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }
    public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }
    public void setCorreo(String correo) { this.correo = correo; }
    public void setNivelInversionista(String nivelInversionista) { this.nivelInversionista = nivelInversionista; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getPasswordHash() { return passwordHash; }
}

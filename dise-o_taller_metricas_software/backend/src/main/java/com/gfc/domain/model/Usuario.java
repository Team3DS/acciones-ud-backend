package com.gfc.domain.model;

import jakarta.persistence.*;

import com.gfc.domain.port.CodificadorContrasena;
import java.util.Objects;

/**
 * Identidad base para iniciar sesion en el sistema, con un {@link Rol} (EMPLEADO o CLIENTE).
 * No existe una subclase para empleados: un empleado es simplemente un Usuario con
 * {@code Rol.EMPLEADO}, ya que no tiene atributos propios. {@link Cliente} si extiende esta
 * clase porque agrega datos adicionales (documento, correo, nombre).
 */
@Entity
@Table(name = "usuarios")
@Inheritance(strategy = InheritanceType.JOINED)
@EntityListeners(com.gfc.infrastructure.security.UsuarioEntityListener.class)
public class Usuario extends BaseEntity {
    /** Requerido por Hibernate. */
    protected Usuario() { }

    @Column(nullable = false, unique = true)
    private String nombreUsuario;
    @Column(nullable = false)
    private String contrasena;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean activo = true;
    @Transient
    private CodificadorContrasena codificador;

    /**
     * Hashea {@code contrasena} con {@code codificador} antes de guardarla; el texto plano nunca
     * se conserva.
     *
     * @throws IllegalArgumentException si nombreUsuario o contrasena estan vacios.
     */
    public Usuario(Long id, String nombreUsuario, String contrasena, Rol rol,
                   CodificadorContrasena codificador) {
        super(id);
        this.nombreUsuario = Validaciones.texto(nombreUsuario, "nombreUsuario");
        Validaciones.texto(contrasena, "contrasena");
        this.rol = Objects.requireNonNull(rol, "rol");
        this.codificador = Objects.requireNonNull(codificador, "codificador");
        this.contrasena = Validaciones.texto(codificador.codificar(contrasena), "hash");
    }

    /** @return true si {@code contraseñaIngresada} coincide con el hash almacenado. */
    public boolean comprobarContraseña(String contraseñaIngresada) {
        return contraseñaIngresada != null && codificador.comprobar(contraseñaIngresada, contrasena);
    }

    public String getNombreUsuario() { return nombreUsuario; }
    /** Restaura el servicio transitorio al cargar la entidad, sin modificar el hash. */
    public void configurarCodificador(CodificadorContrasena codificador) {
        this.codificador = Objects.requireNonNull(codificador, "codificador");
    }
    public Rol getRol() { return rol; }
    public Long getIdUsuario() { return getId(); }
    public boolean isActivo() { return activo; }
    public void darseDeBaja() { this.activo = false; }
}

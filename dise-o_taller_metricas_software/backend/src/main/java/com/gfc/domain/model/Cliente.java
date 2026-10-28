package com.gfc.domain.model;

import jakarta.persistence.*;

import com.gfc.domain.port.CodificadorContrasena;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Un {@link Usuario} con rol fijo {@code CLIENTE} (herencia JOINED) que ademas identifica a la
 * persona con documento, correo y nombre, para poder asociarle {@link Pedido}s.
 */
@Entity
@Table(name = "clientes")
public class Cliente extends Usuario {
    /** Requerido por Hibernate. */
    protected Cliente() { }

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoDocumento tipoDocumento;
    @Column(nullable = false, unique = true)
    private String documento;
    @Column(nullable = false, unique = true)
    private String email;
    @Column(nullable = false)
    private String nombre;

    /** @throws IllegalArgumentException si el email no cumple el formato basico esperado. */
    public Cliente(Long id, String nombreUsuario, String contrasena, TipoDocumento tipoDocumento,
                   String documento, String email, String nombre, CodificadorContrasena codificador) {
        super(id, nombreUsuario, contrasena, Rol.CLIENTE, codificador);
        this.tipoDocumento = Objects.requireNonNull(tipoDocumento, "tipoDocumento");
        this.documento = Validaciones.texto(documento, "documento");
        this.nombre = Validaciones.texto(nombre, "nombre");
        this.email = Validaciones.texto(email, "email");
        if (!EMAIL.matcher(this.email).matches()) {
            throw new IllegalArgumentException("Email invalido");
        }
    }

    public TipoDocumento getTipoDocumento() { return tipoDocumento; }
    public String getDocumento() { return documento; }
    public String getEmail() { return email; }
    public String getNombre() { return nombre; }
    public Long getIdCliente() { return getId(); }
}

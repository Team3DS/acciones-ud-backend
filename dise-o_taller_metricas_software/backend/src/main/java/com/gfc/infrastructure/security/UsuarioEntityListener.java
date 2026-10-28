package com.gfc.infrastructure.security;

import com.gfc.domain.model.Usuario;
import com.gfc.domain.port.CodificadorContrasena;
import jakarta.persistence.PostLoad;
import org.springframework.stereotype.Component;

/** Hibernate obtiene este listener del contenedor de Spring. */
@Component
public class UsuarioEntityListener {
    private final CodificadorContrasena codificador;

    public UsuarioEntityListener(CodificadorContrasena codificador) {
        this.codificador = codificador;
    }

    @PostLoad
    public void restaurarCodificador(Usuario usuario) {
        usuario.configurarCodificador(codificador);
    }
}

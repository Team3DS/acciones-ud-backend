package co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security;

import java.util.Set;

public record UsuarioAutenticado(Long userId, Set<String> roles) {

    public static final String ROL_SISTEMA = "SISTEMA";
    public static final String ROL_SERVICIO_INTERNO = "SERVICIO_INTERNO";

    public UsuarioAutenticado {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }

    public boolean tieneRol(String rol) {
        return roles.contains(rol);
    }
}

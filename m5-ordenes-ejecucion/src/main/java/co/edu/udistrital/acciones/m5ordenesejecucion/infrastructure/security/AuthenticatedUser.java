package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.security;

import java.util.Set;

public record AuthenticatedUser(Long userId, Set<String> roles) {

    public static final String ROL_ADMINISTRADOR = "ADMINISTRADOR";
    public static final String ROL_ADMIN = "ADMIN";
    public static final String ROL_SISTEMA = "SISTEMA";

    public AuthenticatedUser {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }

    public boolean esAdministrador() {
        return roles.contains(ROL_ADMINISTRADOR) || roles.contains(ROL_ADMIN);
    }

    public boolean esSistema() {
        return roles.contains(ROL_SISTEMA);
    }
}

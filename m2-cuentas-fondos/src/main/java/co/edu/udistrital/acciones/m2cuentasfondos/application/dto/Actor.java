package co.edu.udistrital.acciones.m2cuentasfondos.application.dto;

import java.util.Objects;

public record Actor(Long userId, boolean administrador) {

    public static Actor sistema() {
        return new Actor(null, true);
    }

    public boolean esSistema() {
        return userId == null;
    }

    public boolean puedeVer(Long propietarioId) {
        return esSistema() || administrador || Objects.equals(userId, propietarioId);
    }

    public boolean puedeOperar(Long propietarioId) {
        return esSistema() || Objects.equals(userId, propietarioId);
    }
}

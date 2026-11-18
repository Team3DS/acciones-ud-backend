package co.edu.udistrital.acciones.m6portafolio.application.dto;

import java.util.Objects;

public record Actor(Long userId, boolean administrador, boolean sistema) {

    public static Actor interno() {
        return new Actor(null, false, true);
    }

    public boolean puedeVer(Long propietarioId) {
        return sistema || administrador || Objects.equals(userId, propietarioId);
    }

    public boolean puedeOperar(Long propietarioId) {
        return sistema || Objects.equals(userId, propietarioId);
    }
}

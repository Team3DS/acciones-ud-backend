package co.edu.udistrital.acciones.m1identidad.application.dto;

public record RefreshRequest(Long userId, String refreshToken) {
    @Override
    public String toString() { return "RefreshRequest[redacted]"; }
}

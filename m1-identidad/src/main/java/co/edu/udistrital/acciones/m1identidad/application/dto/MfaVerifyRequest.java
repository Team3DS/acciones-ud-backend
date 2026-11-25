package co.edu.udistrital.acciones.m1identidad.application.dto;

public record MfaVerifyRequest(Long userId, String code) {
    @Override
    public String toString() { return "MfaVerifyRequest[redacted]"; }
}

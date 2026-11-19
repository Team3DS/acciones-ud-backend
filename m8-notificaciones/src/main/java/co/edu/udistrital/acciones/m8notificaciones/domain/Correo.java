package co.edu.udistrital.acciones.m8notificaciones.domain;

public record Correo(long userId, String to, String subject, String html) {
    @Override
    public String toString() {
        return "Correo[contenido protegido]";
    }
}

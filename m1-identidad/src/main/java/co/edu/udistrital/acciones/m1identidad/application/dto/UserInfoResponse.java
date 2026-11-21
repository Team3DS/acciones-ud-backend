package co.edu.udistrital.acciones.m1identidad.application.dto;

import java.util.Set;

public record UserInfoResponse(
        Long id,
        String correo,
        String nombres,
        String apellidos,
        String nivelInversionista,
        Set<String> roles,
        String estadoUsuario,
        boolean solicitaReporteMensual
) {}

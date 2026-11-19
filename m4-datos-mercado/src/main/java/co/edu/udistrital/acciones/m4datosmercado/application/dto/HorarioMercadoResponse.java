package co.edu.udistrital.acciones.m4datosmercado.application.dto;

import co.edu.udistrital.acciones.m4datosmercado.domain.HorarioMercado;

import java.time.LocalTime;

public record HorarioMercadoResponse(
        Long id,
        String mercadoMic,
        int diaSemana,
        LocalTime apertura,
        LocalTime cierre,
        LocalTime preMarket,
        LocalTime afterHours) {

    public static HorarioMercadoResponse from(HorarioMercado horario) {
        return new HorarioMercadoResponse(
                horario.getId(),
                horario.getMercadoMic(),
                horario.getDiaSemana(),
                horario.getApertura(),
                horario.getCierre(),
                horario.getPreMarket(),
                horario.getAfterHours()
        );
    }
}
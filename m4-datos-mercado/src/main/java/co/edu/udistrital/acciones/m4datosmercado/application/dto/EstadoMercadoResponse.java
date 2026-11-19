package co.edu.udistrital.acciones.m4datosmercado.application.dto;

import co.edu.udistrital.acciones.m4datosmercado.domain.EstadoMercado;

import java.time.Instant;

public record EstadoMercadoResponse(
        String mercadoMic,
        String estado,
        Instant consultadoEn) {

    public static EstadoMercadoResponse from(String mercadoMic, EstadoMercado estado, Instant consultadoEn) {
        return new EstadoMercadoResponse(mercadoMic, estado.name(), consultadoEn);
    }
}
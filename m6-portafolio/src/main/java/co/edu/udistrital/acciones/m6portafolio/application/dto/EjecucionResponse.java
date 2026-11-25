package co.edu.udistrital.acciones.m6portafolio.application.dto;

import java.math.BigDecimal;

public record EjecucionResponse(boolean applied, PosicionResponse position, BigDecimal averageCost) {
    public EjecucionResponse(boolean applied, PosicionResponse position) {
        this(applied, position, position == null ? null : position.averageCost());
    }
}

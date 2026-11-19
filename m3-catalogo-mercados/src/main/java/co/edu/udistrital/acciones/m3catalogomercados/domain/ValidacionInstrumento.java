package co.edu.udistrital.acciones.m3catalogomercados.domain;

public record ValidacionInstrumento(
        String symbol, boolean valid, MotivoElegibilidad reason, Instrumento instrument) {}

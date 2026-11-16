package co.edu.udistrital.acciones.m3catalogomercados.domain;

public record ResultadoComando<T>(int status, String location, T body, boolean replayed) {}

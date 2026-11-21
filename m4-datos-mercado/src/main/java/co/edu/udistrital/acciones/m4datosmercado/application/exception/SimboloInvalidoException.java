package co.edu.udistrital.acciones.m4datosmercado.application.exception;

public class SimboloInvalidoException extends RuntimeException {
    public SimboloInvalidoException(String symbol) {
        super("Símbolo no válido o no encontrado en catálogo: " + symbol);
    }
}
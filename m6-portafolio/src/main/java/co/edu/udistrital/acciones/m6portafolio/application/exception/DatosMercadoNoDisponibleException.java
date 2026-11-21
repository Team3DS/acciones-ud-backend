package co.edu.udistrital.acciones.m6portafolio.application.exception;

public class DatosMercadoNoDisponibleException extends RuntimeException {

    public DatosMercadoNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}

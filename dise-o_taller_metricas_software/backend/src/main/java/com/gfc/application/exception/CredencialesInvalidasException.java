package com.gfc.application.exception;

/** El GlobalExceptionHandler la traduce a HTTP 401 (login fallido). */
public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException(String mensaje) {
        super(mensaje);
    }
}

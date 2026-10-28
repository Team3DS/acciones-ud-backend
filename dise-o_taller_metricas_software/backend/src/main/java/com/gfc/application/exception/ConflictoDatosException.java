package com.gfc.application.exception;

/** El GlobalExceptionHandler la traduce a HTTP 409 (dato unico duplicado, p. ej. registro). */
public class ConflictoDatosException extends RuntimeException {
    public ConflictoDatosException(String mensaje) {
        super(mensaje);
    }
}

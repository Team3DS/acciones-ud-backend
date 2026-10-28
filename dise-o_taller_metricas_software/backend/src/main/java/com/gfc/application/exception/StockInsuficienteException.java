package com.gfc.application.exception;

/** El GlobalExceptionHandler la traduce a HTTP 409 (no hay stock para preparar un pedido). */
public class StockInsuficienteException extends RuntimeException {
    public StockInsuficienteException(String mensaje) {
        super(mensaje);
    }
}

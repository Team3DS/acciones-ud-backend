package co.edu.udistrital.acciones.common.exception;

public class IdempotencyException extends RuntimeException {
    private final String idempotencyKey;

    public IdempotencyException(String idempotencyKey, String message) {
        super(message);
        this.idempotencyKey = idempotencyKey;
    }

    public String getIdempotencyKey() { return idempotencyKey; }
}
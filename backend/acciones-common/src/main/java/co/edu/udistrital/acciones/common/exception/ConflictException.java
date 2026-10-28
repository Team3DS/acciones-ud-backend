package co.edu.udistrital.acciones.common.exception;

public class ConflictException extends RuntimeException {
    private final String errorCode;
    public ConflictException(String errorCode, String message) { super(message); this.errorCode = errorCode; }
    public String getErrorCode() { return errorCode; }
}
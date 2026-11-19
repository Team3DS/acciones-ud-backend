package co.edu.udistrital.acciones.m3catalogomercados.domain;

public final class CatalogoException extends RuntimeException {
    private final String code;
    private final int status;

    public CatalogoException(String code, int status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String code() {
        return code;
    }

    public int status() {
        return status;
    }
}

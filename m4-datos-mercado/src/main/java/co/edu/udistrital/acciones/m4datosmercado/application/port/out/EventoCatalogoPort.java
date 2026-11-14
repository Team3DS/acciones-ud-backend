package co.edu.udistrital.acciones.m4datosmercado.application.port.out;

public interface EventoCatalogoPort {
    void procesarInstrumentoCreado(String symbol, String name, String market, String currency);
    void procesarInstrumentoActualizado(String symbol, String name, String market, String currency);
    void procesarMercadoCreado(String mic, String name, String timezone, String currency);
    void procesarMercadoActualizado(String mic, String name, String timezone, String currency);
}

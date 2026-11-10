package co.edu.udistrital.acciones.m4datosmercado.application.port.out;

import java.util.List;

public interface CatalogoClientPort {
    boolean simboloExiste(String symbol);
    List<String> validarSimbolos(List<String> symbols);
}

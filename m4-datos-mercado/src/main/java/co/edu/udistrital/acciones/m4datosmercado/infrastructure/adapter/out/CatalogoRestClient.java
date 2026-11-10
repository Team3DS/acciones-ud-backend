package co.edu.udistrital.acciones.m4datosmercado.infrastructure.adapter.out;

import co.edu.udistrital.acciones.m4datosmercado.application.port.out.CatalogoClientPort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CatalogoRestClient implements CatalogoClientPort {
    @Override
    public boolean simboloExiste(String symbol) {
        return true;
    }

    @Override
    public List<String> validarSimbolos(List<String> symbols) {
        return symbols;
    }
}

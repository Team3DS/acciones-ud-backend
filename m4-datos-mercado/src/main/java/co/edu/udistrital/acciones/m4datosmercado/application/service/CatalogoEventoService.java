package co.edu.udistrital.acciones.m4datosmercado.application.service;

import co.edu.udistrital.acciones.m4datosmercado.application.port.out.EventoCatalogoPort;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CatalogoEventoService implements EventoCatalogoPort {

    private final Set<String> simbolosValidos = ConcurrentHashMap.newKeySet();
    private final Map<String, String> mercadoPorSymbol = new ConcurrentHashMap<>();

    @Override
    public void procesarInstrumentoCreado(String symbol, String name, String market, String currency) {
        simbolosValidos.add(symbol.toUpperCase());
        mercadoPorSymbol.put(symbol.toUpperCase(), market);
    }

    @Override
    public void procesarInstrumentoActualizado(String symbol, String name, String market, String currency) {
        simbolosValidos.add(symbol.toUpperCase());
        mercadoPorSymbol.put(symbol.toUpperCase(), market);
    }

    @Override
    public void procesarMercadoCreado(String mic, String name, String timezone, String currency) {
        // Los mercados no afectan directamente a los símbolos, pero se podría usar para filtrar
    }

    @Override
    public void procesarMercadoActualizado(String mic, String name, String timezone, String currency) {
        // Los mercados no afectan directamente a los símbolos
    }

    public boolean simboloExiste(String symbol) {
        return simbolosValidos.contains(symbol.toUpperCase());
    }

    public Set<String> obtenerSimbolosValidos() {
        return Set.copyOf(simbolosValidos);
    }

    public String obtenerMercado(String symbol) {
        return mercadoPorSymbol.get(symbol.toUpperCase());
    }
}

package co.edu.udistrital.acciones.m3catalogomercados.application.port.out;

import co.edu.udistrital.acciones.m3catalogomercados.domain.FiltroInstrumentos;
import co.edu.udistrital.acciones.m3catalogomercados.domain.Instrumento;
import co.edu.udistrital.acciones.m3catalogomercados.domain.Mercado;
import co.edu.udistrital.acciones.m3catalogomercados.domain.Pagina;
import co.edu.udistrital.acciones.m3catalogomercados.domain.Sector;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CatalogoRepositoryPort {
    List<Mercado> listarMercados(Boolean enabled);

    Optional<Mercado> buscarMercado(String code);

    List<Sector> listarSectores();

    Pagina<Instrumento> buscar(FiltroInstrumentos filtro);

    List<Instrumento> buscarPorSimbolos(Collection<String> symbols, String market);

    Optional<Instrumento> buscarPorId(long id);
}

package co.edu.udistrital.acciones.m6portafolio.application.port.out;

import co.edu.udistrital.acciones.m6portafolio.domain.Posicion;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PosicionRepositoryPort {

    Optional<Posicion> findByUserIdAndSymbol(Long userId, String symbol);

    Optional<Posicion> findByUserIdAndSymbolForUpdate(Long userId, String symbol);

    void crearSiNoExiste(Long userId, String symbol, Instant ahora);

    List<Posicion> findAbiertasByUserId(Long userId);

    List<Posicion> findByUserId(Long userId);

    Posicion save(Posicion posicion);
}

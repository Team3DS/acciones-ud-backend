package co.edu.udistrital.acciones.m6portafolio.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m6portafolio.application.port.out.PosicionRepositoryPort;
import co.edu.udistrital.acciones.m6portafolio.domain.Posicion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
interface SpringDataPosicionRepository extends JpaRepository<Posicion, Long> {

    Optional<Posicion> findByUserIdAndSymbol(Long userId, String symbol);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Posicion p where p.userId = :userId and p.symbol = :symbol")
    Optional<Posicion> findByUserIdAndSymbolForUpdate(@Param("userId") Long userId,
                                                      @Param("symbol") String symbol);

    @Query("select p from Posicion p where p.userId = :userId and p.cantidad > 0 order by p.symbol asc")
    List<Posicion> findAbiertasByUserId(@Param("userId") Long userId);

    List<Posicion> findByUserIdOrderBySymbolAsc(Long userId);

    @Modifying
    @Query(value = """
            INSERT INTO posicion (user_id, symbol, cantidad, cantidad_reservada, costo_promedio,
                                  pnl_realizado, creado_en, actualizado_en)
            VALUES (:userId, :symbol, 0, 0, 0, 0, :ahora, :ahora)
            ON CONFLICT (user_id, symbol) DO NOTHING
            """, nativeQuery = true)
    void crearSiNoExiste(@Param("userId") Long userId, @Param("symbol") String symbol,
                         @Param("ahora") Instant ahora);
}

@Repository
public class JpaPosicionRepositoryAdapter implements PosicionRepositoryPort {

    private final SpringDataPosicionRepository repositorio;

    public JpaPosicionRepositoryAdapter(SpringDataPosicionRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public Optional<Posicion> findByUserIdAndSymbol(Long userId, String symbol) {
        return repositorio.findByUserIdAndSymbol(userId, symbol);
    }

    @Override
    public Optional<Posicion> findByUserIdAndSymbolForUpdate(Long userId, String symbol) {
        return repositorio.findByUserIdAndSymbolForUpdate(userId, symbol);
    }

    @Override
    public void crearSiNoExiste(Long userId, String symbol, Instant ahora) {
        repositorio.crearSiNoExiste(userId, symbol, ahora);
    }

    @Override
    public List<Posicion> findAbiertasByUserId(Long userId) {
        return repositorio.findAbiertasByUserId(userId);
    }

    @Override
    public List<Posicion> findByUserId(Long userId) {
        return repositorio.findByUserIdOrderBySymbolAsc(userId);
    }

    @Override
    public Posicion save(Posicion posicion) {
        return repositorio.save(posicion);
    }
}

package co.edu.udistrital.acciones.m4datosmercado.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m4datosmercado.domain.HistoricoCotizacion;
import co.edu.udistrital.acciones.m4datosmercado.domain.Intervalo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface JpaHistoricoRepository extends JpaRepository<HistoricoCotizacion, Long> {

    @Query("SELECT h FROM HistoricoCotizacion h WHERE h.symbol = :symbol AND h.intervalo = :intervalo AND h.timestamp BETWEEN :from AND :to ORDER BY h.timestamp ASC")
    List<HistoricoCotizacion> findBySymbolAndIntervaloAndTimestampBetween(
            @Param("symbol") String symbol,
            @Param("intervalo") Intervalo intervalo,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query("SELECT CASE WHEN COUNT(h) > 0 THEN true ELSE false END FROM HistoricoCotizacion h WHERE h.symbol = :symbol AND h.intervalo = :intervalo AND h.timestamp = :timestamp")
    boolean existsBySymbolAndIntervaloAndTimestamp(
            @Param("symbol") String symbol,
            @Param("intervalo") Intervalo intervalo,
            @Param("timestamp") Instant timestamp);
}
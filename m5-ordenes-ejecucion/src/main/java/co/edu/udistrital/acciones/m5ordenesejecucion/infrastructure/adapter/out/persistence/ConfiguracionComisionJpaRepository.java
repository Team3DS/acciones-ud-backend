package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ConfiguracionComisionJpaRepository extends JpaRepository<ConfiguracionComisionEntity, Long> {

    @Query("SELECT c FROM ConfiguracionComisionEntity c WHERE c.mercadoCodigo = :mercado " +
            "AND c.vigenteDesde <= :momento AND (c.vigenteHasta IS NULL OR c.vigenteHasta > :momento) " +
            "ORDER BY c.vigenteDesde DESC")
    List<ConfiguracionComisionEntity> findVigentesPorMercado(@Param("mercado") String mercado, @Param("momento") Instant momento);

    @Query("SELECT c FROM ConfiguracionComisionEntity c WHERE " +
            "c.vigenteDesde <= :momento AND (c.vigenteHasta IS NULL OR c.vigenteHasta > :momento) " +
            "ORDER BY c.mercadoCodigo ASC")
    List<ConfiguracionComisionEntity> findTodasVigentes(@Param("momento") Instant momento);
}

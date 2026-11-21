package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialEstadoOrdenJpaRepository extends JpaRepository<HistorialEstadoOrdenEntity, Long> {
    List<HistorialEstadoOrdenEntity> findByOrdenIdOrderByOcurridoEnAsc(Long ordenId);
}

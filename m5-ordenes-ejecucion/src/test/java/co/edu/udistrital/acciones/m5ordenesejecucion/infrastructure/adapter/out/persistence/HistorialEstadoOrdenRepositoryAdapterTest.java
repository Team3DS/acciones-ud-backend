package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EstadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.HistorialEstadoOrden;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HistorialEstadoOrdenRepositoryAdapterTest {

    private final HistorialEstadoOrdenJpaRepository jpa = mock(HistorialEstadoOrdenJpaRepository.class);
    private final HistorialEstadoOrdenRepositoryAdapter adapter = new HistorialEstadoOrdenRepositoryAdapter(jpa);

    @Test
    void listaElHistorialEnOrden() {
        when(jpa.findByOrdenIdOrderByOcurridoEnAsc(10L)).thenReturn(List.of(
                EntidadesDePrueba.historial(1L, 10L, EstadoOrden.PENDIENTE),
                EntidadesDePrueba.historial(2L, 10L, EstadoOrden.CANCELADA)));

        assertThat(adapter.findByOrdenId(10L)).extracting(HistorialEstadoOrden::getEstadoOrden)
                .containsExactly(EstadoOrden.PENDIENTE, EstadoOrden.CANCELADA);
    }

    @Test
    void guardaUnCambioDeEstado() {
        HistorialEstadoOrden historial = EntidadesDePrueba.historial(3L, 10L, EstadoOrden.PENDIENTE).toDomain();
        when(jpa.save(any(HistorialEstadoOrdenEntity.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        HistorialEstadoOrden guardado = adapter.save(historial);

        assertThat(guardado.getOrdenId()).isEqualTo(10L);
        assertThat(guardado.getEstadoOrden()).isEqualTo(EstadoOrden.PENDIENTE);
    }
}

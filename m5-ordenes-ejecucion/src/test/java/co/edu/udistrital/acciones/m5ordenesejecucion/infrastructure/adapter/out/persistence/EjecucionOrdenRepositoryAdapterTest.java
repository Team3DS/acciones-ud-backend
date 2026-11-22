package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EjecucionOrden;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EjecucionOrdenRepositoryAdapterTest {

    private EjecucionOrdenJpaRepository jpa;
    private EjecucionOrdenRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        jpa = mock(EjecucionOrdenJpaRepository.class);
        adapter = new EjecucionOrdenRepositoryAdapter(jpa);
    }

    @Test
    void buscaPorIdYPorIdDelBroker() {
        when(jpa.findById(1L)).thenReturn(Optional.of(EntidadesDePrueba.ejecucion(1L, 10L)));
        when(jpa.findByBrokerExecutionId("EXEC-1")).thenReturn(Optional.of(EntidadesDePrueba.ejecucion(1L, 10L)));
        when(jpa.findById(2L)).thenReturn(Optional.empty());

        assertThat(adapter.findById(1L)).map(EjecucionOrden::getOrdenId).contains(10L);
        assertThat(adapter.findByBrokerExecutionId("EXEC-1")).map(EjecucionOrden::getBrokerExecutionId).contains("EXEC-1");
        assertThat(adapter.findById(2L)).isEmpty();
    }

    @Test
    void listaEjecucionesDeUnaOrdenYDeUnUsuario() {
        Instant desde = EntidadesDePrueba.AHORA.minusSeconds(3600);
        Instant hasta = EntidadesDePrueba.AHORA;
        when(jpa.findByOrdenIdOrderByEjecutadaEnAsc(10L)).thenReturn(List.of(EntidadesDePrueba.ejecucion(1L, 10L), EntidadesDePrueba.ejecucion(2L, 10L)));
        when(jpa.findByUsuarioIdAndPeriodo(100L, desde, hasta)).thenReturn(List.of(EntidadesDePrueba.ejecucion(3L, 11L)));

        assertThat(adapter.findByOrdenId(10L)).extracting(EjecucionOrden::getId).containsExactly(1L, 2L);
        assertThat(adapter.findByUsuarioIdAndPeriodo(100L, desde, hasta)).extracting(EjecucionOrden::getOrdenId).containsExactly(11L);
    }

    @Test
    void guardaLaEjecucion() {
        EjecucionOrden ejecucion = EntidadesDePrueba.ejecucion(7L, 10L).toDomain();
        when(jpa.save(any(EjecucionOrdenEntity.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        EjecucionOrden guardada = adapter.save(ejecucion);

        assertThat(guardada.getId()).isEqualTo(7L);
        assertThat(guardada.getCantidad()).isEqualTo(25);
    }
}

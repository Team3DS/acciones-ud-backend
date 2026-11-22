package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m5ordenesejecucion.domain.ConfirmacionEjecucion;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.ServicioParticipante;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfirmacionEjecucionRepositoryAdapterTest {

    private final ConfirmacionEjecucionJpaRepository jpa = mock(ConfirmacionEjecucionJpaRepository.class);
    private final ConfirmacionEjecucionRepositoryAdapter adapter = new ConfirmacionEjecucionRepositoryAdapter(jpa);

    @Test
    void buscaLaConfirmacionDeUnServicio() {
        when(jpa.findById(any(ConfirmacionEjecucionId.class)))
                .thenReturn(Optional.of(EntidadesDePrueba.confirmacion(1L, ServicioParticipante.PORTAFOLIO)));

        Optional<ConfirmacionEjecucion> confirmacion = adapter.findByEjecucionIdAndServicio(1L, ServicioParticipante.PORTAFOLIO);

        assertThat(confirmacion).map(ConfirmacionEjecucion::getServicioParticipante).contains(ServicioParticipante.PORTAFOLIO);
        assertThat(confirmacion).map(ConfirmacionEjecucion::getEjecucionOrdenId).contains(1L);
    }

    @Test
    void listaLasConfirmacionesDeUnaEjecucionYGuarda() {
        when(jpa.findByEjecucionOrdenId(1L)).thenReturn(List.of(EntidadesDePrueba.confirmacion(1L, ServicioParticipante.PORTAFOLIO)));
        when(jpa.save(any(ConfirmacionEjecucionEntity.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        assertThat(adapter.findByEjecucionId(1L)).hasSize(1);
        ConfirmacionEjecucion guardada = adapter.save(EntidadesDePrueba.confirmacion(2L, ServicioParticipante.PORTAFOLIO).toDomain());
        assertThat(guardada.getEjecucionOrdenId()).isEqualTo(2L);
    }
}

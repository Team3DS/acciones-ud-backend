package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m5ordenesejecucion.domain.OutboxEvento;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OutboxEventoRepositoryAdapterTest {

    private final OutboxEventoJpaRepository jpa = mock(OutboxEventoJpaRepository.class);
    private final OutboxEventoRepositoryAdapter adapter = new OutboxEventoRepositoryAdapter(jpa);

    @Test
    void buscaPorId() {
        UUID id = UUID.randomUUID();
        when(jpa.findById(id)).thenReturn(Optional.of(EntidadesDePrueba.outbox(id, "OrdenCreada")));

        assertThat(adapter.findById(id)).map(OutboxEvento::getTipoEvento).contains("OrdenCreada");
    }

    @Test
    void listaPendientesRespetandoElLimite() {
        when(jpa.findPendientes(PageRequest.of(0, 2))).thenReturn(List.of(
                EntidadesDePrueba.outbox(UUID.randomUUID(), "OrdenCreada"),
                EntidadesDePrueba.outbox(UUID.randomUUID(), "OrdenEjecutada")));

        assertThat(adapter.findPendientes(2)).extracting(OutboxEvento::getTipoEvento).containsExactly("OrdenCreada", "OrdenEjecutada");
    }

    @Test
    void guardaElEvento() {
        UUID id = UUID.randomUUID();
        when(jpa.save(any(OutboxEventoEntity.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        OutboxEvento guardado = adapter.save(EntidadesDePrueba.outbox(id, "OrdenCancelada").toDomain());

        assertThat(guardado.getId()).isEqualTo(id);
        assertThat(guardado.getPublicadoEn()).isNull();
    }
}

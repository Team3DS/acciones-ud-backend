package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EstadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.Orden;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrdenRepositoryAdapterTest {

    private OrdenJpaRepository jpa;
    private OrdenRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        jpa = mock(OrdenJpaRepository.class);
        adapter = new OrdenRepositoryAdapter(jpa);
    }

    @Test
    void buscaPorIdYConvierteAlDominio() {
        when(jpa.findById(1L)).thenReturn(Optional.of(EntidadesDePrueba.orden(1L, "AAPL")));

        Optional<Orden> orden = adapter.findById(1L);

        assertThat(orden).isPresent();
        assertThat(orden.get().getId()).isEqualTo(1L);
        assertThat(orden.get().getTicker()).isEqualTo("AAPL");
    }

    @Test
    void devuelveVacioSiNoExiste() {
        when(jpa.findById(9L)).thenReturn(Optional.empty());

        assertThat(adapter.findById(9L)).isEmpty();
    }

    @Test
    void buscaPorIdentificadoresDelBroker() {
        OrdenEntity entity = EntidadesDePrueba.orden(2L, "MSFT");
        UUID clientOrderId = UUID.randomUUID();
        when(jpa.findByClientOrderId(clientOrderId)).thenReturn(Optional.of(entity));
        when(jpa.findByBrokerOrderId("BRK-2")).thenReturn(Optional.of(entity));

        assertThat(adapter.findByClientOrderId(clientOrderId)).map(Orden::getId).contains(2L);
        assertThat(adapter.findByBrokerOrderId("BRK-2")).map(Orden::getTicker).contains("MSFT");
    }

    @Test
    void listaOrdenesDelUsuarioConYSinFiltros() {
        when(jpa.findByUsuarioId(100L)).thenReturn(List.of(EntidadesDePrueba.orden(1L, "AAPL"), EntidadesDePrueba.orden(2L, "MSFT")));
        when(jpa.findByUsuarioIdAndFilters(100L, EstadoOrden.PENDIENTE, "AAPL")).thenReturn(List.of(EntidadesDePrueba.orden(1L, "AAPL")));

        assertThat(adapter.findByUsuarioId(100L)).extracting(Orden::getTicker).containsExactly("AAPL", "MSFT");
        assertThat(adapter.findByUsuarioIdAndFilters(100L, EstadoOrden.PENDIENTE, "AAPL")).extracting(Orden::getId).containsExactly(1L);
    }

    @Test
    void listaOrdenesEncoladasListasParaEnvio() {
        when(jpa.findEncoladasConfirmadas()).thenReturn(List.of(EntidadesDePrueba.orden(3L, "NVDA")));

        assertThat(adapter.findEncoladasListasParaEnvio()).extracting(Orden::getTicker).containsExactly("NVDA");
    }

    @Test
    void guardaYDevuelveLaOrdenPersistida() {
        Orden orden = EntidadesDePrueba.orden(null, "TSLA").toDomain();
        when(jpa.save(any(OrdenEntity.class))).thenAnswer(invocacion -> {
            OrdenEntity guardada = invocacion.getArgument(0);
            guardada.setId(55L);
            return guardada;
        });

        Orden guardada = adapter.save(orden);

        assertThat(guardada.getId()).isEqualTo(55L);
        assertThat(guardada.getTicker()).isEqualTo("TSLA");
        verify(jpa).save(any(OrdenEntity.class));
    }

    @Test
    void listaLasOrdenesEnviadasAlBrokerPendientesDeEjecucion() {
        when(jpa.findByEstadoOrdenInAndBrokerOrderIdIsNotNull(any())).thenReturn(List.of(EntidadesDePrueba.orden(4L, "AMZN")));

        assertThat(adapter.findPendientesDeEjecucion()).extracting(Orden::getTicker).containsExactly("AMZN");
        verify(jpa).findByEstadoOrdenInAndBrokerOrderIdIsNotNull(List.of(
                EstadoOrden.ENVIADA, EstadoOrden.FIRMADA_COMISIONISTA, EstadoOrden.EN_EJECUCION, EstadoOrden.PARCIALMENTE_EJECUTADA));
    }
}

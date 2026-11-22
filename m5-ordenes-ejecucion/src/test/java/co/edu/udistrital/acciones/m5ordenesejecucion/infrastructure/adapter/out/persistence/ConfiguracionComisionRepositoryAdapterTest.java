package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m5ordenesejecucion.domain.ConfiguracionComision;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfiguracionComisionRepositoryAdapterTest {

    private final ConfiguracionComisionJpaRepository jpa = mock(ConfiguracionComisionJpaRepository.class);
    private final ConfiguracionComisionRepositoryAdapter adapter = new ConfiguracionComisionRepositoryAdapter(jpa);

    @Test
    void buscaPorId() {
        when(jpa.findById(1L)).thenReturn(Optional.of(EntidadesDePrueba.comision(1L, "XNYS")));

        assertThat(adapter.findById(1L)).map(ConfiguracionComision::getMercadoCodigo).contains("XNYS");
    }

    @Test
    void tomaLaConfiguracionVigenteMasRecienteDelMercado() {
        Instant momento = EntidadesDePrueba.AHORA;
        when(jpa.findVigentesPorMercado("XNYS", momento)).thenReturn(List.of(
                EntidadesDePrueba.comision(2L, "XNYS"), EntidadesDePrueba.comision(1L, "XNYS")));

        assertThat(adapter.findVigentePorMercado("XNYS", momento)).map(ConfiguracionComision::getId).contains(2L);
    }

    @Test
    void devuelveVacioSiNoHayConfiguracionVigente() {
        Instant momento = EntidadesDePrueba.AHORA;
        when(jpa.findVigentesPorMercado("XNAS", momento)).thenReturn(List.of());

        assertThat(adapter.findVigentePorMercado("XNAS", momento)).isEmpty();
    }

    @Test
    void listaTodasLasVigentesYGuarda() {
        Instant momento = EntidadesDePrueba.AHORA;
        when(jpa.findTodasVigentes(momento)).thenReturn(List.of(EntidadesDePrueba.comision(1L, "XNAS"), EntidadesDePrueba.comision(2L, "XNYS")));
        when(jpa.save(any(ConfiguracionComisionEntity.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        assertThat(adapter.findVigentes(momento)).extracting(ConfiguracionComision::getMercadoCodigo).containsExactly("XNAS", "XNYS");
        ConfiguracionComision guardada = adapter.save(EntidadesDePrueba.comision(3L, "XNAS").toDomain());
        assertThat(guardada.getId()).isEqualTo(3L);
        assertThat(guardada.getPorcentaje()).isEqualByComparingTo("0.02");
    }
}

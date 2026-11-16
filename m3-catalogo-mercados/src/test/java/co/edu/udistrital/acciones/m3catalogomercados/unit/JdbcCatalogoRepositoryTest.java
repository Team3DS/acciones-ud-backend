package co.edu.udistrital.acciones.m3catalogomercados.unit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import co.edu.udistrital.acciones.m3catalogomercados.domain.*;
import co.edu.udistrital.acciones.m3catalogomercados.infrastructure.adapter.out.persistence.JdbcCatalogoRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class JdbcCatalogoRepositoryTest {

    @Mock NamedParameterJdbcTemplate jdbc;
    JdbcCatalogoRepository repository;
    private final Mercado nasdaq =
            new Mercado((short) 1, "NASDAQ", "Nasdaq", "XNAS", "America/New_York", "USD", true);
    private final Sector tecnologia =
            new Sector((short) 1, "TECNOLOGIA", "Tecnología", "Software");
    private final Instrumento aapl =
            new Instrumento(
                    1L,
                    "AAPL",
                    "Apple Inc.",
                    true,
                    nasdaq,
                    tecnologia,
                    TipoInstrumento.ACCION,
                    new BigDecimal("100"),
                    new BigDecimal("0.01"),
                    EstadoInstrumento.ACTIVO,
                    java.time.Instant.now(),
                    java.time.Instant.now());

    @BeforeEach
    void setup() {
        repository = new JdbcCatalogoRepository(jdbc);
    }

    @SuppressWarnings("unchecked")
    @Test
    void listarMercadosConFiltroEnabled() {
        when(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of(nasdaq));
        var result = repository.listarMercados(true);
        assertThat(result).containsExactly(nasdaq);
        verify(jdbc).query(contains("WHERE"), any(SqlParameterSource.class), any(RowMapper.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void listarMercadosSinFiltro() {
        when(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of(nasdaq));
        var result = repository.listarMercados(null);
        assertThat(result).containsExactly(nasdaq);
        verify(jdbc).query(contains("WHERE (:enabled IS NULL"), any(SqlParameterSource.class), any(RowMapper.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void buscarMercadoPorCodigoEncontrado() {
        when(jdbc.query(anyString(), any(Map.class), any(RowMapper.class)))
                .thenReturn(List.of(nasdaq));
        var result = repository.buscarMercado("NASDAQ");
        assertThat(result).isPresent().contains(nasdaq);
        verify(jdbc).query(contains("m.codigo = :code OR m.codigo_mic = :code"), any(Map.class), any(RowMapper.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void buscarMercadoPorCodigoNoEncontrado() {
        when(jdbc.query(anyString(), any(Map.class), any(RowMapper.class)))
                .thenReturn(List.of());
        var result = repository.buscarMercado("UNKNOWN");
        assertThat(result).isEmpty();
    }

    @SuppressWarnings("unchecked")
    @Test
    void listarSectores() {
        when(jdbc.query(anyString(), any(RowMapper.class)))
                .thenReturn(List.of(tecnologia));
        var result = repository.listarSectores();
        assertThat(result).containsExactly(tecnologia);
    }

    @SuppressWarnings("unchecked")
    @Test
    void buscarConPaginacionYFiltros() {
        var filtro = new FiltroInstrumentos(
                "Apple", "NASDAQ", "TECNOLOGIA", true, 0, 10, OrdenCatalogo.COMPANY_NAME, true);
        when(jdbc.queryForObject(anyString(), any(SqlParameterSource.class), eq(Long.class)))
                .thenReturn(1L);
        when(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of(aapl));
        var result = repository.buscar(filtro);
        assertThat(result.content()).containsExactly(aapl);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalElements()).isOne();
        assertThat(result.totalPages()).isOne();
        verify(jdbc).queryForObject(contains("COUNT"), any(SqlParameterSource.class), eq(Long.class));
        verify(jdbc).query(contains("ORDER BY"), any(SqlParameterSource.class), any(RowMapper.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void buscarSinFiltrosPaginaCero() {
        var filtro = new FiltroInstrumentos(
                null, null, null, null, 0, 20, OrdenCatalogo.SYMBOL, false);
        when(jdbc.queryForObject(anyString(), any(SqlParameterSource.class), eq(Long.class)))
                .thenReturn(0L);
        when(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());
        var result = repository.buscar(filtro);
        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
        assertThat(result.totalPages()).isZero();
    }

    @SuppressWarnings("unchecked")
    @Test
    void buscarConQueryEscapaCaracteresEspeciales() {
        var filtro = new FiltroInstrumentos(
                "A!P%L_", null, null, null, 0, 10, OrdenCatalogo.SYMBOL, false);
        when(jdbc.queryForObject(anyString(), any(SqlParameterSource.class), eq(Long.class)))
                .thenReturn(0L);
        when(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());
        repository.buscar(filtro);
        verify(jdbc).query(anyString(), argThat((SqlParameterSource params) -> {
            String query = ((MapSqlParameterSource) params).getValue("query").toString();
            return query.contains("A!!P!%L!_");
        }), any(RowMapper.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void buscarPorSimbolosVacioRetornaListaVacia() {
        var result = repository.buscarPorSimbolos(List.of(), "NASDAQ");
        assertThat(result).isEmpty();
        verifyNoInteractions(jdbc);
    }

    @SuppressWarnings("unchecked")
    @Test
    void buscarPorSimbolosConMercado() {
        when(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of(aapl));
        var result = repository.buscarPorSimbolos(List.of("AAPL", "GOOGL"), "NASDAQ");
        assertThat(result).containsExactly(aapl);
        verify(jdbc).query(contains("a.ticker IN"), any(SqlParameterSource.class), any(RowMapper.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void buscarPorIdEncontrado() {
        when(jdbc.query(anyString(), any(Map.class), any(RowMapper.class)))
                .thenReturn(List.of(aapl));
        var result = repository.buscarPorId(1L);
        assertThat(result).isPresent().contains(aapl);
    }

    @SuppressWarnings("unchecked")
    @Test
    void buscarPorIdNoEncontrado() {
        when(jdbc.query(anyString(), any(Map.class), any(RowMapper.class)))
                .thenReturn(List.of());
        var result = repository.buscarPorId(999L);
        assertThat(result).isEmpty();
    }
}
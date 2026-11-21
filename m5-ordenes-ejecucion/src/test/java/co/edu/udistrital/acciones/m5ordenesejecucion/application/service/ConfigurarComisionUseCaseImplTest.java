package co.edu.udistrital.acciones.m5ordenesejecucion.application.service;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.ConfigurarComisionUseCase;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.ConfiguracionComisionRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.ConfiguracionComision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;

@ExtendWith(MockitoExtension.class)
class ConfigurarComisionUseCaseImplTest {

    @Mock
    ConfiguracionComisionRepositoryPort comisionRepository;

    private ConfigurarComisionUseCaseImpl service;

    @BeforeEach
    void setUp() {
        service = new ConfigurarComisionUseCaseImpl(comisionRepository);
    }

    @Test
    void configurarComisionValida() {
        ConfiguracionComision anterior = new ConfiguracionComision();
        anterior.setId(1L);
        anterior.setMercadoCodigo("NYSE");
        anterior.setVigenteHasta(null);
        anterior.setVigenteDesde(Instant.now().minusSeconds(100));

        when(comisionRepository.findVigentePorMercado(eq("NYSE"), any(Instant.class))).thenReturn(Optional.of(anterior));
        when(comisionRepository.save(any(ConfiguracionComision.class))).thenAnswer(inv -> inv.getArgument(0));

        ConfigurarComisionUseCase.ConfigurarComisionCommand cmd = new ConfigurarComisionUseCase.ConfigurarComisionCommand(
                "NYSE", new BigDecimal("2.50"), "Ajuste de mercado", 1L
        );

        ConfiguracionComision resultado = service.configurar(cmd);

        assertEquals("NYSE", resultado.getMercadoCodigo());
        assertEquals(new BigDecimal("2.50"), resultado.getPorcentaje());
        assertEquals("Ajuste de mercado", resultado.getMotivo());
        assertEquals(1L, resultado.getCreadoPorUsuarioId());
        verify(comisionRepository, times(2)).save(any(ConfiguracionComision.class));
    }

    @Test
    void configurarComisionPorcentajeNegativoLanzaExcepcion() {
        ConfigurarComisionUseCase.ConfigurarComisionCommand cmd = new ConfigurarComisionUseCase.ConfigurarComisionCommand(
                "NYSE", new BigDecimal("-1.0"), null, 1L
        );

        assertThrows(IllegalArgumentException.class, () -> service.configurar(cmd));
        verifyNoInteractions(comisionRepository);
    }

    @Test
    void configurarComisionPorcentajeSobreCienLanzaExcepcion() {
        ConfigurarComisionUseCase.ConfigurarComisionCommand cmd = new ConfigurarComisionUseCase.ConfigurarComisionCommand(
                "NYSE", new BigDecimal("101.0"), null, 1L
        );

        assertThrows(IllegalArgumentException.class, () -> service.configurar(cmd));
        verifyNoInteractions(comisionRepository);
    }

    @Test
    void configurarComisionPorcentajeNuloLanzaExcepcion() {
        ConfigurarComisionUseCase.ConfigurarComisionCommand cmd = new ConfigurarComisionUseCase.ConfigurarComisionCommand(
                "NYSE", null, null, 1L
        );

        assertThrows(IllegalArgumentException.class, () -> service.configurar(cmd));
        verifyNoInteractions(comisionRepository);
    }

    @Test
    void listarVigentesRetornaLista() {
        ConfiguracionComision cfg = new ConfiguracionComision();
        cfg.setId(1L);
        when(comisionRepository.findVigentes(any(Instant.class))).thenReturn(List.of(cfg));

        List<ConfiguracionComision> resultado = service.listarVigentes();

        assertEquals(1, resultado.size());
        verify(comisionRepository).findVigentes(any(Instant.class));
    }
}

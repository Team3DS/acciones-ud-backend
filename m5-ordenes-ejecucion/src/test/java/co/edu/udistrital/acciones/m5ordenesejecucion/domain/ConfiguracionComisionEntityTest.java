package co.edu.udistrital.acciones.m5ordenesejecucion.domain;

import co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.persistence.ConfiguracionComisionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ConfiguracionComisionEntityTest {

    private ConfiguracionComisionEntity entity;
    private Instant ahora;

    @BeforeEach
    void setUp() {
        ahora = Instant.now();
        entity = new ConfiguracionComisionEntity();
        entity.setId(1L);
        entity.setMercadoCodigo("NASDAQ");
        entity.setPorcentaje(new BigDecimal("0.0025"));
        entity.setVigenteDesde(ahora.minusSeconds(86400));
        entity.setVigenteHasta(ahora.plusSeconds(86400));
        entity.setMotivo("Comision estandar");
        entity.setCreadoPorUsuarioId(100L);
        entity.setCreadoEn(ahora);
    }

    @Test
    void gettersYSetters() {
        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getMercadoCodigo()).isEqualTo("NASDAQ");
        assertThat(entity.getPorcentaje()).isEqualTo(new BigDecimal("0.0025"));
        assertThat(entity.getVigenteDesde()).isEqualTo(ahora.minusSeconds(86400));
        assertThat(entity.getVigenteHasta()).isEqualTo(ahora.plusSeconds(86400));
        assertThat(entity.getMotivo()).isEqualTo("Comision estandar");
        assertThat(entity.getCreadoPorUsuarioId()).isEqualTo(100L);
        assertThat(entity.getCreadoEn()).isEqualTo(ahora);
    }

    @Test
    void toDomain() {
        var domain = entity.toDomain();

        assertThat(domain.getId()).isEqualTo(1L);
        assertThat(domain.getMercadoCodigo()).isEqualTo("NASDAQ");
        assertThat(domain.getPorcentaje()).isEqualTo(new BigDecimal("0.0025"));
        assertThat(domain.getVigenteDesde()).isEqualTo(ahora.minusSeconds(86400));
        assertThat(domain.getVigenteHasta()).isEqualTo(ahora.plusSeconds(86400));
        assertThat(domain.getMotivo()).isEqualTo("Comision estandar");
        assertThat(domain.getCreadoPorUsuarioId()).isEqualTo(100L);
        assertThat(domain.getCreadoEn()).isEqualTo(ahora);
    }

    @Test
    void fromDomain() {
        var domain = new co.edu.udistrital.acciones.m5ordenesejecucion.domain.ConfiguracionComision(
                1L, "NASDAQ", new BigDecimal("0.0025"),
                ahora.minusSeconds(86400), ahora.plusSeconds(86400),
                "Comision estandar", 100L, ahora
        );

        ConfiguracionComisionEntity entityFromDomain = ConfiguracionComisionEntity.fromDomain(domain);

        assertThat(entityFromDomain.getId()).isEqualTo(1L);
        assertThat(entityFromDomain.getMercadoCodigo()).isEqualTo("NASDAQ");
        assertThat(entityFromDomain.getPorcentaje()).isEqualTo(new BigDecimal("0.0025"));
        assertThat(entityFromDomain.getVigenteDesde()).isEqualTo(ahora.minusSeconds(86400));
        assertThat(entityFromDomain.getVigenteHasta()).isEqualTo(ahora.plusSeconds(86400));
        assertThat(entityFromDomain.getMotivo()).isEqualTo("Comision estandar");
        assertThat(entityFromDomain.getCreadoPorUsuarioId()).isEqualTo(100L);
        assertThat(entityFromDomain.getCreadoEn()).isEqualTo(ahora);
    }
}
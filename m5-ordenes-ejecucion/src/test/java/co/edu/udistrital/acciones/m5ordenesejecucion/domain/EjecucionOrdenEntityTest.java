package co.edu.udistrital.acciones.m5ordenesejecucion.domain;

import co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.persistence.EjecucionOrdenEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EjecucionOrdenEntityTest {

    private EjecucionOrdenEntity entity;
    private Instant ahora;

    @BeforeEach
    void setUp() {
        ahora = Instant.now();
        entity = new EjecucionOrdenEntity();
        entity.setId(1L);
        entity.setOrdenId(100L);
        entity.setBrokerExecutionId("EXEC123");
        entity.setCantidad(25);
        entity.setPrecioEjecucion(new BigDecimal("150.2500"));
        entity.setComision(new BigDecimal("5.00"));
        entity.setEjecutadaEn(ahora);
        entity.setRegistradaEn(ahora);
    }

    @Test
    void gettersYSetters() {
        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getOrdenId()).isEqualTo(100L);
        assertThat(entity.getBrokerExecutionId()).isEqualTo("EXEC123");
        assertThat(entity.getCantidad()).isEqualTo(25);
        assertThat(entity.getPrecioEjecucion()).isEqualTo(new BigDecimal("150.2500"));
        assertThat(entity.getComision()).isEqualTo(new BigDecimal("5.00"));
        assertThat(entity.getEjecutadaEn()).isEqualTo(ahora);
        assertThat(entity.getRegistradaEn()).isEqualTo(ahora);
    }

    @Test
    void toDomain() {
        EjecucionOrden domain = entity.toDomain();

        assertThat(domain.getId()).isEqualTo(1L);
        assertThat(domain.getOrdenId()).isEqualTo(100L);
        assertThat(domain.getBrokerExecutionId()).isEqualTo("EXEC123");
        assertThat(domain.getCantidad()).isEqualTo(25);
        assertThat(domain.getPrecioEjecucion()).isEqualTo(new BigDecimal("150.2500"));
        assertThat(domain.getComision()).isEqualTo(new BigDecimal("5.00"));
        assertThat(domain.getEjecutadaEn()).isEqualTo(ahora);
        assertThat(domain.getRegistradaEn()).isEqualTo(ahora);
    }

    @Test
    void fromDomain() {
        EjecucionOrden domain = new EjecucionOrden(
                1L, 100L, "EXEC123", 25,
                new BigDecimal("150.2500"), new BigDecimal("5.00"),
                ahora, ahora
        );

        EjecucionOrdenEntity entityFromDomain = EjecucionOrdenEntity.fromDomain(domain);

        assertThat(entityFromDomain.getId()).isEqualTo(1L);
        assertThat(entityFromDomain.getOrdenId()).isEqualTo(100L);
        assertThat(entityFromDomain.getBrokerExecutionId()).isEqualTo("EXEC123");
        assertThat(entityFromDomain.getCantidad()).isEqualTo(25);
        assertThat(entityFromDomain.getPrecioEjecucion()).isEqualTo(new BigDecimal("150.2500"));
        assertThat(entityFromDomain.getComision()).isEqualTo(new BigDecimal("5.00"));
        assertThat(entityFromDomain.getEjecutadaEn()).isEqualTo(ahora);
        assertThat(entityFromDomain.getRegistradaEn()).isEqualTo(ahora);
    }
}
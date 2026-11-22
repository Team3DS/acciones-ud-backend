package co.edu.udistrital.acciones.m5ordenesejecucion.domain;

import co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.persistence.ConfirmacionEjecucionEntity;
import co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.persistence.ConfirmacionEjecucionId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ConfirmacionEjecucionEntityTest {

    private ConfirmacionEjecucionEntity entity;
    private Instant ahora;

    @BeforeEach
    void setUp() {
        ahora = Instant.now();
        entity = new ConfirmacionEjecucionEntity();
        entity.setId(new ConfirmacionEjecucionId(1L, ServicioParticipante.PORTAFOLIO));
        entity.setPrecioPromedioCosto(new BigDecimal("150.1234"));
        entity.setConfirmadaEn(ahora);
    }

    @Test
    void gettersYSetters() {
        assertThat(entity.getId().getEjecucionOrdenId()).isEqualTo(1L);
        assertThat(entity.getId().getServicioParticipante()).isEqualTo(ServicioParticipante.PORTAFOLIO);
        assertThat(entity.getPrecioPromedioCosto()).isEqualTo(new BigDecimal("150.1234"));
        assertThat(entity.getConfirmadaEn()).isEqualTo(ahora);
    }

    @Test
    void toDomain() {
        var domain = entity.toDomain();

        assertThat(domain.getEjecucionOrdenId()).isEqualTo(1L);
        assertThat(domain.getServicioParticipante()).isEqualTo(ServicioParticipante.PORTAFOLIO);
        assertThat(domain.getPrecioPromedioCosto()).isEqualTo(new BigDecimal("150.1234"));
        assertThat(domain.getConfirmadaEn()).isEqualTo(ahora);
    }

    @Test
    void fromDomain() {
        var domain = new ConfirmacionEjecucion(
                1L, ServicioParticipante.FONDOS,
                new BigDecimal("149.5000"), ahora
        );

        ConfirmacionEjecucionEntity entityFromDomain = ConfirmacionEjecucionEntity.fromDomain(domain);

        assertThat(entityFromDomain.getId().getEjecucionOrdenId()).isEqualTo(1L);
        assertThat(entityFromDomain.getId().getServicioParticipante()).isEqualTo(ServicioParticipante.FONDOS);
        assertThat(entityFromDomain.getPrecioPromedioCosto()).isEqualTo(new BigDecimal("149.5000"));
        assertThat(entityFromDomain.getConfirmadaEn()).isEqualTo(ahora);
    }
}
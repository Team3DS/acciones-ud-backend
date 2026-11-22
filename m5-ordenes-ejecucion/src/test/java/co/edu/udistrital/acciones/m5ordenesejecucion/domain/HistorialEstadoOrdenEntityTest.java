package co.edu.udistrital.acciones.m5ordenesejecucion.domain;

import co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.persistence.HistorialEstadoOrdenEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class HistorialEstadoOrdenEntityTest {

    private HistorialEstadoOrdenEntity entity;
    private Instant ahora;

    @BeforeEach
    void setUp() {
        ahora = Instant.now();
        entity = new HistorialEstadoOrdenEntity();
        entity.setId(1L);
        entity.setOrdenId(100L);
        entity.setEstadoOrden(EstadoOrden.PENDIENTE);
        entity.setTipoActor(TipoActor.USUARIO);
        entity.setActorUsuarioId(50L);
        entity.setCausa("Orden creada por usuario");
        entity.setOcurridoEn(ahora);
    }

    @Test
    void gettersYSetters() {
        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getOrdenId()).isEqualTo(100L);
        assertThat(entity.getEstadoOrden()).isEqualTo(EstadoOrden.PENDIENTE);
        assertThat(entity.getTipoActor()).isEqualTo(TipoActor.USUARIO);
        assertThat(entity.getActorUsuarioId()).isEqualTo(50L);
        assertThat(entity.getCausa()).isEqualTo("Orden creada por usuario");
        assertThat(entity.getOcurridoEn()).isEqualTo(ahora);
    }

    @Test
    void toDomain() {
        var domain = entity.toDomain();

        assertThat(domain.getId()).isEqualTo(1L);
        assertThat(domain.getOrdenId()).isEqualTo(100L);
        assertThat(domain.getEstadoOrden()).isEqualTo(EstadoOrden.PENDIENTE);
        assertThat(domain.getTipoActor()).isEqualTo(TipoActor.USUARIO);
        assertThat(domain.getActorUsuarioId()).isEqualTo(50L);
        assertThat(domain.getCausa()).isEqualTo("Orden creada por usuario");
        assertThat(domain.getOcurridoEn()).isEqualTo(ahora);
    }

    @Test
    void fromDomain() {
        var domain = new HistorialEstadoOrden(
                1L, 100L, EstadoOrden.PENDIENTE, TipoActor.USUARIO,
                50L, "Orden creada por usuario", ahora
        );

        HistorialEstadoOrdenEntity entityFromDomain = HistorialEstadoOrdenEntity.fromDomain(domain);

        assertThat(entityFromDomain.getId()).isEqualTo(1L);
        assertThat(entityFromDomain.getOrdenId()).isEqualTo(100L);
        assertThat(entityFromDomain.getEstadoOrden()).isEqualTo(EstadoOrden.PENDIENTE);
        assertThat(entityFromDomain.getTipoActor()).isEqualTo(TipoActor.USUARIO);
        assertThat(entityFromDomain.getActorUsuarioId()).isEqualTo(50L);
        assertThat(entityFromDomain.getCausa()).isEqualTo("Orden creada por usuario");
        assertThat(entityFromDomain.getOcurridoEn()).isEqualTo(ahora);
    }
}
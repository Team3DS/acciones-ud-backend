package co.edu.udistrital.acciones.m5ordenesejecucion.domain;

import co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.persistence.OutboxEventoEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class OutboxEventoEntityTest {

    private OutboxEventoEntity entity;
    private Instant ahora;
    private UUID id;

    @BeforeEach
    void setUp() {
        ahora = Instant.now();
        id = UUID.randomUUID();
        entity = new OutboxEventoEntity();
        entity.setId(id);
        entity.setTipoAgregado("Orden");
        entity.setAgregadoId("1");
        entity.setTipoEvento("OrdenCreada");
        entity.setCarga("{\"ordenId\":1}");
        entity.setCreadoEn(ahora);
        entity.setPublicadoEn(null);
        entity.setIntentos(0);
        entity.setUltimoError(null);
    }

    @Test
    void gettersYSetters() {
        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getTipoAgregado()).isEqualTo("Orden");
        assertThat(entity.getAgregadoId()).isEqualTo("1");
        assertThat(entity.getTipoEvento()).isEqualTo("OrdenCreada");
        assertThat(entity.getCarga()).isEqualTo("{\"ordenId\":1}");
        assertThat(entity.getCreadoEn()).isEqualTo(ahora);
        assertThat(entity.getPublicadoEn()).isNull();
        assertThat(entity.getIntentos()).isEqualTo(0);
        assertThat(entity.getUltimoError()).isNull();
    }

    @Test
    void marcarPublicado() {
        Instant publicadoEn = ahora.plusSeconds(60);

        entity.setPublicadoEn(publicadoEn);
        entity.setIntentos(1);

        assertThat(entity.getPublicadoEn()).isEqualTo(publicadoEn);
        assertThat(entity.getIntentos()).isEqualTo(1);
    }

    @Test
    void registrarFallo() {
        String error = "Timeout de conexion";
        Integer nuevosIntentos = 3;

        entity.setUltimoError(error);
        entity.setIntentos(nuevosIntentos);

        assertThat(entity.getUltimoError()).isEqualTo(error);
        assertThat(entity.getIntentos()).isEqualTo(nuevosIntentos);
    }

    @Test
    void toDomain() {
        OutboxEvento domain = entity.toDomain();

        assertThat(domain.getId()).isEqualTo(id);
        assertThat(domain.getTipoAgregado()).isEqualTo("Orden");
        assertThat(domain.getAgregadoId()).isEqualTo("1");
        assertThat(domain.getTipoEvento()).isEqualTo("OrdenCreada");
        assertThat(domain.getCarga()).isEqualTo("{\"ordenId\":1}");
        assertThat(domain.getCreadoEn()).isEqualTo(ahora);
        assertThat(domain.getPublicadoEn()).isNull();
        assertThat(domain.getIntentos()).isEqualTo(0);
        assertThat(domain.getUltimoError()).isNull();
    }

    @Test
    void fromDomain() {
        OutboxEvento domain = new OutboxEvento(
                id, "Orden", "1", "OrdenCreada",
                "{\"ordenId\":1}", ahora, null, 0, null
        );

        OutboxEventoEntity entityFromDomain = OutboxEventoEntity.fromDomain(domain);

        assertThat(entityFromDomain.getId()).isEqualTo(id);
        assertThat(entityFromDomain.getTipoAgregado()).isEqualTo("Orden");
        assertThat(entityFromDomain.getAgregadoId()).isEqualTo("1");
        assertThat(entityFromDomain.getTipoEvento()).isEqualTo("OrdenCreada");
        assertThat(entityFromDomain.getCarga()).isEqualTo("{\"ordenId\":1}");
        assertThat(entityFromDomain.getCreadoEn()).isEqualTo(ahora);
        assertThat(entityFromDomain.getPublicadoEn()).isNull();
        assertThat(entityFromDomain.getIntentos()).isEqualTo(0);
        assertThat(entityFromDomain.getUltimoError()).isNull();
    }
}
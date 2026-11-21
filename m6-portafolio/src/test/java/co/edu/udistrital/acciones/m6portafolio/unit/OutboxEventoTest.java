package co.edu.udistrital.acciones.m6portafolio.unit;

import co.edu.udistrital.acciones.m6portafolio.domain.OutboxEvento;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxEventoTest {

    private static final UUID ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final Instant CREADO = Instant.parse("2026-03-04T10:00:00Z");
    private static final Instant PUBLICADO = CREADO.plusSeconds(2);

    @Test
    void registraUnEventoPendienteConCeroReintentos() {
        OutboxEvento evento = OutboxEvento.registrar(ID, "posicion", "77", "m6.posiciones.actualizadas",
                "{\"symbol\":\"AAPL\"}", "corr-1", CREADO);

        assertThat(evento.getId()).isEqualTo(ID);
        assertThat(evento.getAggregateType()).isEqualTo("posicion");
        assertThat(evento.getAggregateId()).isEqualTo("77");
        assertThat(evento.getEventType()).isEqualTo("m6.posiciones.actualizadas");
        assertThat(evento.getPayload()).isEqualTo("{\"symbol\":\"AAPL\"}");
        assertThat(evento.getCorrelationId()).isEqualTo("corr-1");
        assertThat(evento.getCreadoEn()).isEqualTo(CREADO);
        assertThat(evento.getProcesadoEn()).isNull();
        assertThat(evento.isPublicado()).isFalse();
        assertThat(evento.getReintentos()).isZero();
        assertThat(evento.getUltimoError()).isNull();
    }

    @Test
    void marcarPublicadoRegistraElInstanteYLimpiaElError() {
        OutboxEvento evento = evento();
        evento.anotarFallo("broker caido");

        evento.publicar(PUBLICADO);

        assertThat(evento.isPublicado()).isTrue();
        assertThat(evento.getProcesadoEn()).isEqualTo(PUBLICADO);
        assertThat(evento.getUltimoError()).isNull();
        assertThat(evento.getReintentos()).isEqualTo(1);
    }

    @Test
    void publicarDosVecesConservaElPrimerInstante() {
        OutboxEvento evento = evento();

        evento.publicar(PUBLICADO);
        evento.publicar(PUBLICADO.plusSeconds(30));

        assertThat(evento.getProcesadoEn()).isEqualTo(PUBLICADO);
    }

    @Test
    void anotarFalloAcumulaReintentosYGuardaElUltimoError() {
        OutboxEvento evento = evento();

        evento.anotarFallo("timeout");
        evento.anotarFallo("conexion rechazada");

        assertThat(evento.isPublicado()).isFalse();
        assertThat(evento.getReintentos()).isEqualTo(2);
        assertThat(evento.getUltimoError()).isEqualTo("conexion rechazada");
    }

    @Test
    void exigeElIdYLosTextosObligatorios() {
        assertThatThrownBy(() -> OutboxEvento.registrar(null, "posicion", "77", "tipo", "{}", null, CREADO))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> OutboxEvento.registrar(ID, " ", "77", "tipo", "{}", null, CREADO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OutboxEvento.registrar(ID, "posicion", null, "tipo", "{}", null, CREADO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OutboxEvento.registrar(ID, "posicion", "77", "", "{}", null, CREADO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OutboxEvento.registrar(ID, "posicion", "77", "tipo", " ", null, CREADO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OutboxEvento.registrar(ID, "posicion", "77", "tipo", "{}", null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void exigeElInstanteAlPublicar() {
        OutboxEvento evento = evento();

        assertThatThrownBy(() -> evento.publicar(null))
                .isInstanceOf(NullPointerException.class);
    }

    private static OutboxEvento evento() {
        return OutboxEvento.registrar(ID, "posicion", "77", "m6.posiciones.actualizadas",
                "{}", "corr-1", CREADO);
    }
}

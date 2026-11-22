package co.edu.udistrital.acciones.m10suscripcionespagos.unit;

import co.edu.udistrital.acciones.m10suscripcionespagos.domain.EventoStripe;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventoStripeTest {

    private static final Instant AHORA = Instant.parse("2026-05-01T10:00:00Z");

    @Test
    void registrarDejaElEventoPendienteSinIntentos() {
        EventoStripe evento = EventoStripe.registrar("evt_1", "invoice.paid", "in_1", "{}", AHORA);

        assertThat(evento.getId()).isEqualTo("evt_1");
        assertThat(evento.getTipo()).isEqualTo("invoice.paid");
        assertThat(evento.getObjetoId()).isEqualTo("in_1");
        assertThat(evento.getRecibidoEn()).isEqualTo(AHORA);
        assertThat(evento.getSiguienteIntentoEn()).isEqualTo(AHORA);
        assertThat(evento.getIntentos()).isZero();
        assertThat(evento.getProcesadoEn()).isNull();
        assertThat(evento.estaProcesado()).isFalse();
        assertThat(evento.estaPendiente(AHORA, 10)).isTrue();
    }

    @Test
    void registrarFalloSumaUnIntentoYProgramaElReintento() {
        EventoStripe evento = EventoStripe.registrar("evt_1", "invoice.paid", "in_1", "{}", AHORA);

        evento.registrarFallo("Stripe no responde", AHORA, Duration.ofSeconds(30));

        assertThat(evento.getIntentos()).isEqualTo(1);
        assertThat(evento.getUltimoError()).isEqualTo("Stripe no responde");
        assertThat(evento.getSiguienteIntentoEn()).isEqualTo(AHORA.plusSeconds(30));
        assertThat(evento.estaPendiente(AHORA.plusSeconds(29), 10)).isFalse();
        assertThat(evento.estaPendiente(AHORA.plusSeconds(30), 10)).isTrue();
    }

    @Test
    void marcarProcesadoLimpiaElUltimoError() {
        EventoStripe evento = EventoStripe.registrar("evt_1", "invoice.paid", "in_1", "{}", AHORA);
        evento.registrarFallo("fallo", AHORA, Duration.ofSeconds(1));

        evento.marcarProcesado(AHORA.plusSeconds(5));

        assertThat(evento.estaProcesado()).isTrue();
        assertThat(evento.getProcesadoEn()).isEqualTo(AHORA.plusSeconds(5));
        assertThat(evento.getUltimoError()).isNull();
    }

    @Test
    void unEventoProcesadoNoVuelveAAceptarFallos() {
        EventoStripe evento = EventoStripe.registrar("evt_1", "invoice.paid", "in_1", "{}", AHORA);
        evento.marcarProcesado(AHORA);

        evento.registrarFallo("tarde", AHORA.plusSeconds(1), Duration.ofSeconds(1));

        assertThat(evento.getIntentos()).isZero();
        assertThat(evento.getUltimoError()).isNull();
        assertThat(evento.getSiguienteIntentoEn()).isEqualTo(AHORA);
    }

    @Test
    void unEventoConElTopeDeIntentosNoEstaPendiente() {
        EventoStripe evento = EventoStripe.registrar("evt_1", "invoice.paid", "in_1", "{}", AHORA);
        for (int intento = 0; intento < 3; intento++) {
            evento.registrarFallo("fallo", AHORA, Duration.ofSeconds(1));
        }

        assertThat(evento.getIntentos()).isEqualTo(3);
        assertThat(evento.estaPendiente(AHORA.plusSeconds(60), 3)).isFalse();
        assertThat(evento.estaPendiente(AHORA.plusSeconds(60), 4)).isTrue();
    }

    @Test
    void registrarRechazaUnIdVacio() {
        assertThatThrownBy(() -> EventoStripe.registrar(" ", "invoice.paid", "in_1", "{}", AHORA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void registrarFalloRechazaUnBackoffNoPositivo() {
        EventoStripe evento = EventoStripe.registrar("evt_1", "invoice.paid", "in_1", "{}", AHORA);

        assertThatThrownBy(() -> evento.registrarFallo("fallo", AHORA, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

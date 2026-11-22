package co.edu.udistrital.acciones.m10suscripcionespagos.unit;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.EventoStripeRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.ManejadorEventoStripePort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.usecase.ProcesarEventosStripeUseCase;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.EventoStripe;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.config.WebhooksProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcesarEventosStripeUseCaseTest {

    private static final Instant AHORA = Instant.parse("2026-05-01T10:00:00Z");

    private final EventoStripeRepositoryPort eventos = mock(EventoStripeRepositoryPort.class);
    private final PlatformTransactionManager gestor = mock(PlatformTransactionManager.class);
    private final Clock clock = Clock.fixed(AHORA, ZoneOffset.UTC);

    @BeforeEach
    void preparar() {
        when(gestor.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(new SimpleTransactionStatus());
        when(eventos.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    void unFalloNoImpideProcesarLosDemasEventos() {
        EventoStripe falla = EventoStripe.registrar("evt_falla", "invoice.paid", "in_1", "{}", AHORA);
        EventoStripe correcto = EventoStripe.registrar("evt_ok", "invoice.paid", "in_2", "{}", AHORA);
        prepararPendientes(List.of("evt_falla", "evt_ok"));
        when(eventos.bloquearParaProcesar(eq("evt_falla"), eq(AHORA), eq(3)))
                .thenReturn(Optional.of(falla));
        when(eventos.bloquearParaProcesar(eq("evt_ok"), eq(AHORA), eq(3)))
                .thenReturn(Optional.of(correcto));
        when(eventos.findById("evt_falla")).thenReturn(Optional.of(falla));

        ProcesarEventosStripeUseCase useCase = new ProcesarEventosStripeUseCase(
                gestor, eventos, List.of(manejadorQueFalla("evt_falla")), propiedades(3,
                        Duration.ofSeconds(30), Duration.ofHours(6)), clock);

        useCase.procesarPendientes();

        assertThat(falla.getIntentos()).isEqualTo(1);
        assertThat(falla.getUltimoError()).contains("boom");
        assertThat(falla.getSiguienteIntentoEn()).isEqualTo(AHORA.plusSeconds(30));
        assertThat(falla.estaProcesado()).isFalse();
        assertThat(correcto.estaProcesado()).isTrue();
        assertThat(correcto.getProcesadoEn()).isEqualTo(AHORA);
    }

    @Test
    void elBackoffCreceYSeCapaEnElMaximo() {
        EventoStripe evento = EventoStripe.registrar("evt_falla", "invoice.paid", "in_1", "{}", AHORA);
        for (int intento = 0; intento < 4; intento++) {
            evento.registrarFallo("previo", AHORA, Duration.ofSeconds(1));
        }
        prepararPendientes(List.of("evt_falla"));
        when(eventos.bloquearParaProcesar(eq("evt_falla"), eq(AHORA), eq(10)))
                .thenReturn(Optional.of(evento));
        when(eventos.findById("evt_falla")).thenReturn(Optional.of(evento));

        ProcesarEventosStripeUseCase useCase = new ProcesarEventosStripeUseCase(
                gestor, eventos, List.of(manejadorQueFalla("evt_falla")), propiedades(10,
                        Duration.ofHours(1), Duration.ofHours(6)), clock);

        useCase.procesarPendientes();

        assertThat(evento.getIntentos()).isEqualTo(5);
        assertThat(evento.getSiguienteIntentoEn()).isEqualTo(AHORA.plus(Duration.ofHours(6)));
    }

    @Test
    void unEventoAjenoSeMarcaProcesadoConUnAviso() {
        EventoStripe evento = EventoStripe.registrar("evt_ajeno", "customer.subscription.updated",
                "sub_1", "{}", AHORA);
        prepararPendientes(List.of("evt_ajeno"));
        when(eventos.bloquearParaProcesar(eq("evt_ajeno"), eq(AHORA), eq(3)))
                .thenReturn(Optional.of(evento));

        ProcesarEventosStripeUseCase useCase = new ProcesarEventosStripeUseCase(
                gestor, eventos, List.of(), propiedades(3, Duration.ofSeconds(30), Duration.ofHours(6)),
                clock);

        useCase.procesarPendientes();

        assertThat(evento.estaProcesado()).isTrue();
        verify(eventos).save(evento);
    }

    @Test
    void noProcesaNadaCuandoNoHayPendientes() {
        prepararPendientes(List.of());

        ProcesarEventosStripeUseCase useCase = new ProcesarEventosStripeUseCase(
                gestor, eventos, List.of(), propiedades(3, Duration.ofSeconds(30), Duration.ofHours(6)),
                clock);

        useCase.procesarPendientes();

        verify(eventos, never()).bloquearParaProcesar(any(), any(), eq(3));
        verify(eventos, never()).save(any());
    }

    @Test
    void unEventoTomadoPorOtroProcesadorSeOmite() {
        prepararPendientes(List.of("evt_tomado"));
        when(eventos.bloquearParaProcesar(eq("evt_tomado"), eq(AHORA), eq(3)))
                .thenReturn(Optional.empty());

        ProcesarEventosStripeUseCase useCase = new ProcesarEventosStripeUseCase(
                gestor, eventos, List.of(), propiedades(3, Duration.ofSeconds(30), Duration.ofHours(6)),
                clock);

        useCase.procesarPendientes();

        verify(eventos, never()).save(any());
    }

    private void prepararPendientes(List<String> ids) {
        when(eventos.findPendientes(AHORA, 3, 50)).thenReturn(ids);
        when(eventos.findPendientes(AHORA, 10, 50)).thenReturn(ids);
    }

    private static WebhooksProperties propiedades(int maxIntentos, Duration base, Duration maximo) {
        return new WebhooksProperties(true, Duration.ofSeconds(10), 50, maxIntentos, base, maximo);
    }

    private static ManejadorEventoStripePort manejadorQueFalla(String idQueFalla) {
        return new ManejadorEventoStripePort() {
            @Override
            public boolean soporta(String tipo) {
                return true;
            }

            @Override
            public void procesar(EventoStripe evento) {
                if (idQueFalla.equals(evento.getId())) {
                    throw new IllegalStateException("boom");
                }
            }
        };
    }
}

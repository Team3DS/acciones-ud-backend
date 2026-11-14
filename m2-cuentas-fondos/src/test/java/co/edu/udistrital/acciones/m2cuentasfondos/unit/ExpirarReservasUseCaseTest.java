package co.edu.udistrital.acciones.m2cuentasfondos.unit;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.FondosLiberadosPayload;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.RoutingKeys;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.CuentaRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.MovimientoRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.OutboxPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.ReservaRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.service.LibroMovimientos;
import co.edu.udistrital.acciones.m2cuentasfondos.application.usecase.ExpirarReservasUseCase;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Cuenta;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.EstadoReserva;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Movimiento;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Reserva;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.TipoMovimiento;
import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.config.ReservasProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpirarReservasUseCaseTest {

    private static final Instant AHORA = Instant.parse("2026-11-11T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(AHORA, ZoneOffset.UTC);
    private static final Duration TTL = Duration.ofMinutes(10);
    private static final int LOTE = 50;

    @Mock
    private ReservaRepositoryPort reservas;

    @Mock
    private CuentaRepositoryPort cuentas;

    @Mock
    private MovimientoRepositoryPort movimientos;

    @Mock
    private OutboxPort outbox;

    @Mock
    private PlatformTransactionManager transactionManager;

    private ReservasProperties properties;

    private ExpirarReservasUseCase casoDeUso;

    @BeforeEach
    void preparar() {
        properties = new ReservasProperties();
        properties.setTtl(TTL);
        properties.getExpiracion().setLote(LOTE);
        lenient().when(transactionManager.getTransaction(any()))
                .thenAnswer(invocacion -> new SimpleTransactionStatus());        casoDeUso = new ExpirarReservasUseCase(reservas, cuentas,
                new LibroMovimientos(movimientos, outbox, CLOCK), outbox,
                new TransactionTemplate(transactionManager), properties, CLOCK);
    }

    @Test
    void expiraSoloLasReservasQueSiguenVencidasYPendientes() {
        Reserva vencida = reserva(77L, 10L, AHORA.minus(TTL).minusSeconds(60));
        Reserva reciente = reserva(88L, 10L, AHORA.minus(TTL).plusSeconds(60));
        cuandoHayVencidas(vencida, reciente);
        when(reservas.findByIdForUpdate(77L)).thenReturn(Optional.of(vencida));
        when(reservas.findByIdForUpdate(88L)).thenReturn(Optional.of(reciente));
        Cuenta cuenta = cuentaConReserva(10L, 5L, "700.00", "300.00");
        when(cuentas.findByIdForUpdate(10L)).thenReturn(Optional.of(cuenta));
        when(movimientos.save(any(Movimiento.class))).thenAnswer(invocacion -> {
            Movimiento movimiento = invocacion.getArgument(0);
            ReflectionTestUtils.setField(movimiento, "id", 99L);
            return movimiento;
        });

        int expiradas = casoDeUso.ejecutar();

        assertThat(expiradas).isEqualTo(1);
        assertThat(vencida.getEstado()).isEqualTo(EstadoReserva.EXPIRED);
        assertThat(vencida.getMotivoLiberacion()).isEqualTo(ExpirarReservasUseCase.MOTIVO);
        assertThat(reciente.getEstado()).isEqualTo(EstadoReserva.PENDING);
        assertThat(cuenta.getSaldoDisponible()).isEqualByComparingTo("1000.00");
        assertThat(cuenta.getSaldoReservado()).isEqualByComparingTo("0.00");
        verify(reservas).findVencidas(AHORA.minus(TTL), LOTE);
        verify(cuentas).save(cuenta);
        verify(reservas).save(vencida);
        verify(reservas, never()).save(reciente);

        ArgumentCaptor<Movimiento> captorMovimiento = ArgumentCaptor.forClass(Movimiento.class);
        verify(movimientos).save(captorMovimiento.capture());
        assertThat(captorMovimiento.getValue().getTipo()).isEqualTo(TipoMovimiento.LIBERACION);
        assertThat(captorMovimiento.getValue().getIdempotencyKey()).isEqualTo("LIB:77");

        ArgumentCaptor<Object> captorEvento = ArgumentCaptor.forClass(Object.class);
        verify(outbox).registrar(eq("Reserva"), eq("77"), eq(RoutingKeys.FONDOS_LIBERADOS),
                captorEvento.capture());
        assertThat(captorEvento.getValue()).isEqualTo(new FondosLiberadosPayload(77L, "EXPIRADA"));
    }

    @Test
    void unaReservaResueltaEntreLaConsultaYElBloqueoSeSalta() {
        Reserva vencida = reserva(77L, 10L, AHORA.minus(TTL).minusSeconds(60));
        vencida.confirmar("ejecucion-1", AHORA);
        cuandoHayVencidas(vencida);
        when(reservas.findByIdForUpdate(77L)).thenReturn(Optional.of(vencida));
        when(cuentas.findByIdForUpdate(10L)).thenReturn(Optional.of(
                cuentaConReserva(10L, 5L, "700.00", "300.00")));

        int expiradas = casoDeUso.ejecutar();

        assertThat(expiradas).isZero();
        assertThat(vencida.getEstado()).isEqualTo(EstadoReserva.CONFIRMED);
        verify(cuentas, never()).save(any());
        verify(reservas, never()).save(any());
        verify(movimientos, never()).save(any());
        verify(outbox, never()).registrar(any(), any(), any(), any());
    }

    @Test
    void unFalloNoDetieneLaExpiracionDeLasDemas() {
        Reserva primera = reserva(77L, 10L, AHORA.minus(TTL).minusSeconds(120));
        Reserva segunda = reserva(88L, 20L, AHORA.minus(TTL).minusSeconds(60));
        cuandoHayVencidas(primera, segunda);
        when(reservas.findByIdForUpdate(88L)).thenReturn(Optional.of(segunda));
        when(cuentas.findByIdForUpdate(10L)).thenThrow(new IllegalStateException("cuenta bloqueada"));
        Cuenta cuenta = cuentaConReserva(20L, 6L, "700.00", "300.00");
        when(cuentas.findByIdForUpdate(20L)).thenReturn(Optional.of(cuenta));
        when(movimientos.save(any(Movimiento.class))).thenAnswer(invocacion -> {
            Movimiento movimiento = invocacion.getArgument(0);
            ReflectionTestUtils.setField(movimiento, "id", 100L);
            return movimiento;
        });

        int expiradas = casoDeUso.ejecutar();

        assertThat(expiradas).isEqualTo(1);
        assertThat(primera.getEstado()).isEqualTo(EstadoReserva.PENDING);
        assertThat(segunda.getEstado()).isEqualTo(EstadoReserva.EXPIRED);
        verify(transactionManager, times(1)).rollback(any());
        verify(transactionManager, times(1)).commit(any());
        verify(reservas, never()).save(primera);
        verify(reservas).save(segunda);
    }

    @Test
    void sinReservasVencidasNoBloqueaCuentas() {
        cuandoHayVencidas();

        int expiradas = casoDeUso.ejecutar();

        assertThat(expiradas).isZero();
        verifyNoInteractions(cuentas, movimientos, outbox);
        verify(reservas, never()).save(any());
    }

    private void cuandoHayVencidas(Reserva... vencidas) {
        when(reservas.findVencidas(AHORA.minus(TTL), LOTE)).thenReturn(List.of(vencidas));
        for (Reserva reserva : vencidas) {
            when(reservas.findById(reserva.getId())).thenReturn(Optional.of(reserva));
        }
    }

    private Reserva reserva(Long id, Long cuentaId, Instant creadoEn) {
        Reserva reserva = Reserva.crear(cuentaId, "orden-" + id, new BigDecimal("300.00"),
                "RES:clave-" + id, creadoEn);
        ReflectionTestUtils.setField(reserva, "id", id);
        return reserva;
    }

    private Cuenta cuentaConReserva(Long id, Long userId, String disponible, String reservado) {
        Cuenta cuenta = Cuenta.crear(userId, AHORA);
        ReflectionTestUtils.setField(cuenta, "id", id);
        cuenta.abonar(new BigDecimal(disponible).add(new BigDecimal(reservado)), AHORA);
        cuenta.reservar(new BigDecimal(reservado), AHORA);
        return cuenta;
    }
}

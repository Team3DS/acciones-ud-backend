package co.edu.udistrital.acciones.m2cuentasfondos.unit;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.Actor;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.ReservaResponse;
import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.AccesoDenegadoException;
import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.RecursoNoEncontradoException;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.CuentaRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.ReservaRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.usecase.ConsultarReservaUseCase;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Cuenta;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Reserva;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultarReservaUseCaseTest {

    private static final Instant AHORA = Instant.parse("2026-11-11T10:00:00Z");

    @Mock
    private CuentaRepositoryPort cuentas;

    @Mock
    private ReservaRepositoryPort reservas;

    @InjectMocks
    private ConsultarReservaUseCase casoDeUso;

    @Test
    void elDuenoConsultaSuReserva() {
        Reserva reserva = reserva(77L, 10L, "orden-1", new BigDecimal("300.00"));
        Cuenta cuenta = cuenta(10L, 5L);
        cuenta.abonar(new BigDecimal("1000.00"), AHORA);
        cuenta.reservar(new BigDecimal("300.00"), AHORA);
        when(reservas.findById(77L)).thenReturn(Optional.of(reserva));
        when(cuentas.findById(10L)).thenReturn(Optional.of(cuenta));

        ReservaResponse respuesta = casoDeUso.ejecutar(77L, new Actor(5L, false));

        assertThat(respuesta.reservationId()).isEqualTo(77L);
        assertThat(respuesta.orderId()).isEqualTo("orden-1");
        assertThat(respuesta.userId()).isEqualTo(5L);
        assertThat(respuesta.amount()).isEqualByComparingTo("300.00");
        assertThat(respuesta.status()).isEqualTo("PENDING");
        assertThat(respuesta.available()).isEqualByComparingTo("700.00");
        assertThat(respuesta.reserved()).isEqualByComparingTo("300.00");
        assertThat(respuesta.total()).isEqualByComparingTo("1000.00");
        assertThat(respuesta.currency()).isEqualTo("USD");
        assertThat(respuesta.createdAt()).isEqualTo(AHORA);
    }

    @Test
    void unAdministradorPuedeConsultarLaReservaDeOtroUsuario() {
        when(reservas.findById(77L)).thenReturn(Optional.of(reserva(77L, 10L, "orden-1", new BigDecimal("300.00"))));
        when(cuentas.findById(10L)).thenReturn(Optional.of(cuenta(10L, 5L)));

        ReservaResponse respuesta = casoDeUso.ejecutar(77L, new Actor(1L, true));

        assertThat(respuesta.userId()).isEqualTo(5L);
    }

    @Test
    void unUsuarioAjenoNoPuedeConsultarLaReserva() {
        when(reservas.findById(77L)).thenReturn(Optional.of(reserva(77L, 10L, "orden-1", new BigDecimal("300.00"))));
        when(cuentas.findById(10L)).thenReturn(Optional.of(cuenta(10L, 5L)));

        assertThatThrownBy(() -> casoDeUso.ejecutar(77L, new Actor(9L, false)))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    void responde404SiNoExisteLaReserva() {
        when(reservas.findById(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> casoDeUso.ejecutar(77L, new Actor(5L, false)))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("77");

        verifyNoInteractions(cuentas);
    }

    @Test
    void responde404SiNoExisteLaCuentaDeLaReserva() {
        when(reservas.findById(77L)).thenReturn(Optional.of(reserva(77L, 10L, "orden-1", new BigDecimal("300.00"))));
        when(cuentas.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> casoDeUso.ejecutar(77L, new Actor(5L, false)))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("77");
    }

    private Cuenta cuenta(Long id, Long userId) {
        Cuenta cuenta = Cuenta.crear(userId, AHORA);
        ReflectionTestUtils.setField(cuenta, "id", id);
        return cuenta;
    }

    private Reserva reserva(Long id, Long cuentaId, String orden, BigDecimal monto) {
        Reserva reserva = Reserva.crear(cuentaId, orden, monto, "RES:clave-" + id, AHORA);
        ReflectionTestUtils.setField(reserva, "id", id);
        return reserva;
    }
}

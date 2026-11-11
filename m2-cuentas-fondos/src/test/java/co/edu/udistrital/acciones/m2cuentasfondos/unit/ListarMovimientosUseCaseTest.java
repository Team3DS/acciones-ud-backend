package co.edu.udistrital.acciones.m2cuentasfondos.unit;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.Actor;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.FiltroMovimientos;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.MovimientoResponse;
import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.AccesoDenegadoException;
import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.OrdenInvalidoException;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.CuentaRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.MovimientoRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.usecase.ListarMovimientosUseCase;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Cuenta;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Movimiento;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.TipoMovimiento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListarMovimientosUseCaseTest {

    private static final Instant AHORA = Instant.parse("2026-11-11T10:00:00Z");
    private static final FiltroMovimientos SIN_FILTRO = new FiltroMovimientos(null, null, null);

    @Mock
    private CuentaRepositoryPort cuentas;

    @Mock
    private MovimientoRepositoryPort movimientos;

    private ListarMovimientosUseCase casoDeUso;

    @BeforeEach
    void preparar() {
        casoDeUso = new ListarMovimientosUseCase(cuentas, movimientos);
    }

    @Test
    void devuelveUnaPaginaVaciaSiElUsuarioNoTieneCuenta() {
        when(cuentas.findByUserId(5L)).thenReturn(Optional.empty());

        Page<MovimientoResponse> pagina = casoDeUso.ejecutar(5L, SIN_FILTRO, PageRequest.of(0, 20),
                new Actor(5L, false));

        assertThat(pagina).isEmpty();
        assertThat(pagina.getTotalElements()).isZero();
        assertThat(pagina.getNumber()).isZero();
        assertThat(pagina.getSize()).isEqualTo(20);
        verifyNoInteractions(movimientos);
    }

    @Test
    void mapeaLosMovimientosDeLaCuenta() {
        Cuenta cuenta = cuenta(10L, 5L);
        Movimiento movimiento = movimiento(77L, 10L, TipoMovimiento.RETIRO, new BigDecimal("200.00"),
                new BigDecimal("800.00"));
        when(cuentas.findByUserId(5L)).thenReturn(Optional.of(cuenta));
        when(movimientos.buscar(eq(10L), eq(SIN_FILTRO), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(movimiento), PageRequest.of(0, 20), 1));

        Page<MovimientoResponse> pagina = casoDeUso.ejecutar(5L, SIN_FILTRO, PageRequest.of(0, 20),
                new Actor(5L, false));

        assertThat(pagina.getTotalElements()).isEqualTo(1);
        MovimientoResponse respuesta = pagina.getContent().get(0);
        assertThat(respuesta.movementId()).isEqualTo(77L);
        assertThat(respuesta.type()).isEqualTo("RETIRO");
        assertThat(respuesta.amount()).isEqualByComparingTo("200.00");
        assertThat(respuesta.balanceAfter()).isEqualByComparingTo("800.00");
        assertThat(respuesta.reference()).isEqualTo("RETIRO");
        assertThat(respuesta.createdAt()).isEqualTo(AHORA);
    }

    @Test
    void ordenaPorCreadoEnDescendenteCuandoNoSeIndicaOrden() {
        when(cuentas.findByUserId(5L)).thenReturn(Optional.of(cuenta(10L, 5L)));
        when(movimientos.buscar(eq(10L), any(), any())).thenReturn(Page.empty());
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

        casoDeUso.ejecutar(5L, SIN_FILTRO, PageRequest.of(0, 20), new Actor(5L, false));

        verify(movimientos).buscar(eq(10L), any(), captor.capture());
        assertThat(captor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "creadoEn"));
    }

    @Test
    void traduceLosCamposDeOrdenPublicosALosDeLaEntidad() {
        when(cuentas.findByUserId(5L)).thenReturn(Optional.of(cuenta(10L, 5L)));
        when(movimientos.buscar(eq(10L), any(), any())).thenReturn(Page.empty());
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

        casoDeUso.ejecutar(5L, SIN_FILTRO,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "amount")), new Actor(5L, false));

        verify(movimientos).buscar(eq(10L), any(), captor.capture());
        assertThat(captor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "monto"));
    }

    @Test
    void rechazaUnOrdenNoPermitido() {
        PageRequest pageable = PageRequest.of(0, 20, Sort.by("descripcion"));

        assertThatThrownBy(() -> casoDeUso.ejecutar(5L, SIN_FILTRO, pageable, new Actor(5L, false)))
                .isInstanceOf(OrdenInvalidoException.class)
                .hasMessageContaining("descripcion");

        verifyNoInteractions(cuentas, movimientos);
    }

    @Test
    void validaElOrdenAunqueElUsuarioNoTengaCuenta() {
        PageRequest pageable = PageRequest.of(0, 20, Sort.by("descripcion"));

        assertThatThrownBy(() -> casoDeUso.ejecutar(5L, SIN_FILTRO, pageable, new Actor(5L, false)))
                .isInstanceOf(OrdenInvalidoException.class);

        verify(cuentas, never()).findByUserId(any());
    }

    @Test
    void unUsuarioAjenoNoPuedeVerLosMovimientos() {
        assertThatThrownBy(() -> casoDeUso.ejecutar(5L, SIN_FILTRO, PageRequest.of(0, 20),
                new Actor(9L, false)))
                .isInstanceOf(AccesoDenegadoException.class);

        verifyNoInteractions(cuentas, movimientos);
    }

    @Test
    void unAdministradorPuedeVerLosMovimientosDeOtroUsuario() {
        when(cuentas.findByUserId(5L)).thenReturn(Optional.of(cuenta(10L, 5L)));
        when(movimientos.buscar(eq(10L), any(), any())).thenReturn(Page.empty());

        Page<MovimientoResponse> pagina = casoDeUso.ejecutar(5L, SIN_FILTRO, PageRequest.of(0, 20),
                new Actor(1L, true));

        assertThat(pagina).isEmpty();
    }

    private Cuenta cuenta(Long id, Long userId) {
        Cuenta cuenta = Cuenta.crear(userId, AHORA);
        ReflectionTestUtils.setField(cuenta, "id", id);
        return cuenta;
    }

    private Movimiento movimiento(Long id, Long cuentaId, TipoMovimiento tipo, BigDecimal monto,
                                  BigDecimal saldoPosterior) {
        Movimiento movimiento = Movimiento.crear(cuentaId, tipo, monto, saldoPosterior,
                tipo.name(), "clave-" + id, "descripción", AHORA);
        ReflectionTestUtils.setField(movimiento, "id", id);
        return movimiento;
    }
}

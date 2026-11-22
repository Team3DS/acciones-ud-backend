package co.edu.udistrital.acciones.m10suscripcionespagos.unit;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.ConflictoEstadoException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.ProveedorPagosNoDisponibleException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.RecursoNoEncontradoException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.SuscripcionAjenaException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.BloqueoUsuarioPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.PaymentProviderPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.SuscripcionRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.proveedor.SuscripcionProveedor;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.service.SincronizadorSuscripciones;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.usecase.CancelarSuscripcionUseCase;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.DatosSuscripcion;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.EstadoSuscripcion;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.IntervaloPlan;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.Plan;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.PrecioPlan;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.Suscripcion;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.config.StripeProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CancelarSuscripcionUseCaseTest {

    private static final long USUARIO = 81001L;
    private static final long ID = 7L;
    private static final Instant INICIO = Instant.parse("2026-03-01T00:00:00Z");
    private static final Instant FIN = Instant.parse("2026-04-01T00:00:00Z");

    private final SuscripcionRepositoryPort suscripciones = mock(SuscripcionRepositoryPort.class);
    private final PaymentProviderPort proveedor = mock(PaymentProviderPort.class);
    private final SincronizadorSuscripciones sincronizador = mock(SincronizadorSuscripciones.class);
    private final BloqueoUsuarioPort bloqueo = mock(BloqueoUsuarioPort.class);
    private final StripeProperties propiedades = new StripeProperties(
            "sk_test_clave", "whsec_secreto", null, null, null, 2, null, null, null, null, null);

    private CancelarSuscripcionUseCase useCase;

    @BeforeEach
    void preparar() {
        useCase = new CancelarSuscripcionUseCase(suscripciones, proveedor, sincronizador, bloqueo, propiedades);
        when(proveedor.programarCancelacion(anyString(), anyString())).thenReturn(remota());
        when(sincronizador.aplicar(any())).thenAnswer(invocacion -> entidad(invocacion.getArgument(0)));
    }

    @Test
    void programaLaCancelacionYAplicaLaRespuestaDeStripe() {
        when(suscripciones.findById(ID)).thenReturn(Optional.of(suscripcion(EstadoSuscripcion.ACTIVA, false)));

        Suscripcion resultado = useCase.ejecutar(USUARIO, ID);

        assertThat(resultado.isCancelarAlFinal()).isTrue();
        verify(bloqueo).bloquear(USUARIO);
        verify(proveedor).programarCancelacion("sub_test", "m10-cancelar-7-3");
        verify(sincronizador).aplicar(any());
    }

    @Test
    void permiteProgramarLaCancelacionEnMora() {
        when(suscripciones.findById(ID)).thenReturn(Optional.of(suscripcion(EstadoSuscripcion.EN_MORA, false)));

        useCase.ejecutar(USUARIO, ID);

        verify(proveedor).programarCancelacion(anyString(), anyString());
    }

    @Test
    void unaCancelacionYaProgramadaNoLlamaAStripe() {
        when(suscripciones.findById(ID)).thenReturn(Optional.of(suscripcion(EstadoSuscripcion.ACTIVA, true)));

        Suscripcion resultado = useCase.ejecutar(USUARIO, ID);

        assertThat(resultado.isCancelarAlFinal()).isTrue();
        verify(proveedor, never()).programarCancelacion(anyString(), anyString());
        verify(sincronizador, never()).aplicar(any());
    }

    @Test
    void unaSuscripcionAjenaEs403() {
        when(suscripciones.findById(ID)).thenReturn(Optional.of(suscripcion(EstadoSuscripcion.ACTIVA, false)));

        assertThatThrownBy(() -> useCase.ejecutar(USUARIO + 1, ID))
                .isInstanceOf(SuscripcionAjenaException.class);
        verify(proveedor, never()).programarCancelacion(anyString(), anyString());
    }

    @Test
    void unaSuscripcionInexistenteEs404() {
        when(suscripciones.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.ejecutar(USUARIO, ID))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void unEstadoQueNoPermiteCancelarEs409() {
        when(suscripciones.findById(ID)).thenReturn(Optional.of(suscripcion(EstadoSuscripcion.CANCELADA, false)));

        assertThatThrownBy(() -> useCase.ejecutar(USUARIO, ID))
                .isInstanceOf(ConflictoEstadoException.class);
        verify(proveedor, never()).programarCancelacion(anyString(), anyString());
    }

    @Test
    void siStripeFallaNoSincronizaNada() {
        when(suscripciones.findById(ID)).thenReturn(Optional.of(suscripcion(EstadoSuscripcion.ACTIVA, false)));
        when(proveedor.programarCancelacion(anyString(), anyString()))
                .thenThrow(new ProveedorPagosNoDisponibleException("Stripe no esta disponible"));

        assertThatThrownBy(() -> useCase.ejecutar(USUARIO, ID))
                .isInstanceOf(ProveedorPagosNoDisponibleException.class);
        verify(sincronizador, never()).aplicar(any());
        verify(suscripciones, never()).save(any());
    }

    @Test
    void siStripeNoEstaConfiguradoNoBloquea() {
        StripeProperties sinClave = new StripeProperties(
                "", "", null, null, null, 2, null, null, null, null, null);
        CancelarSuscripcionUseCase sinStripe = new CancelarSuscripcionUseCase(
                suscripciones, proveedor, sincronizador, bloqueo, sinClave);

        assertThatThrownBy(() -> sinStripe.ejecutar(USUARIO, ID))
                .isInstanceOf(ProveedorPagosNoDisponibleException.class);
        verify(bloqueo, never()).bloquear(any());
    }

    private static Suscripcion suscripcion(EstadoSuscripcion estado, boolean cancelarAlFinal) {
        Plan plan = Plan.crear("PREMIUM", "Acciones UD Premium", "Watchlist y alertas");
        PrecioPlan precio = PrecioPlan.crear(plan, IntervaloPlan.MENSUAL, new BigDecimal("12.00"),
                "USD", "acciones_ud_premium_mensual");
        Suscripcion suscripcion = Suscripcion.crear(USUARIO, new DatosSuscripcion(
                "sub_test", precio, estado, INICIO, FIN, cancelarAlFinal, null, null), INICIO);
        asignar(suscripcion, "id", ID);
        asignar(suscripcion, "version", 3L);
        return suscripcion;
    }

    private static SuscripcionProveedor remota() {
        return new SuscripcionProveedor("sub_test", "active", true, null, null, null,
                USUARIO, "cus_test", "acciones_ud_premium_mensual", INICIO, FIN, "si_1");
    }

    private static Suscripcion entidad(SuscripcionProveedor remota) {
        Plan plan = Plan.crear("PREMIUM", "Acciones UD Premium", "Watchlist y alertas");
        PrecioPlan precio = PrecioPlan.crear(plan, IntervaloPlan.MENSUAL, new BigDecimal("12.00"),
                "USD", remota.lookupKey());
        Suscripcion entidad = Suscripcion.crear(USUARIO, new DatosSuscripcion(
                remota.id(), precio, EstadoSuscripcion.ACTIVA, remota.periodoInicio(),
                remota.periodoFin(), remota.cancelarAlFinal(), null, null), INICIO);
        asignar(entidad, "id", ID);
        return entidad;
    }

    private static void asignar(Suscripcion suscripcion, String campo, Object valor) {
        try {
            java.lang.reflect.Field atributo = Suscripcion.class.getDeclaredField(campo);
            atributo.setAccessible(true);
            atributo.set(suscripcion, valor);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}

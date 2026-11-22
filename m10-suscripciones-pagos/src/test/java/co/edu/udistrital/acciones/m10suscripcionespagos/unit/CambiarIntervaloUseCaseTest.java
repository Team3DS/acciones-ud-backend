package co.edu.udistrital.acciones.m10suscripcionespagos.unit;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.CatalogoInconsistenteException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.ConflictoEstadoException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.ProveedorPagosNoDisponibleException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.RecursoNoEncontradoException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.SuscripcionAjenaException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.BloqueoUsuarioPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.PaymentProviderPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.PlanRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.SuscripcionRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.proveedor.PrecioProveedor;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.proveedor.SuscripcionProveedor;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.service.CatalogoPreciosProveedor;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.service.SincronizadorSuscripciones;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.usecase.CambiarIntervaloUseCase;
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
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CambiarIntervaloUseCaseTest {

    private static final long USUARIO = 82001L;
    private static final long ID = 8L;
    private static final Instant AHORA = Instant.parse("2026-03-01T00:00:00Z");
    private static final Instant FIN = Instant.parse("2026-04-01T00:00:00Z");

    private final SuscripcionRepositoryPort suscripciones = mock(SuscripcionRepositoryPort.class);
    private final PlanRepositoryPort planes = mock(PlanRepositoryPort.class);
    private final PaymentProviderPort proveedor = mock(PaymentProviderPort.class);
    private final SincronizadorSuscripciones sincronizador = mock(SincronizadorSuscripciones.class);
    private final BloqueoUsuarioPort bloqueo = mock(BloqueoUsuarioPort.class);
    private final StripeProperties propiedades = new StripeProperties(
            "sk_test_clave", "whsec_secreto", null, null, null, 2, null, null, null, null, null);
    private final CatalogoPreciosProveedor catalogo = new CatalogoPreciosProveedor(
            proveedor, propiedades, Clock.fixed(AHORA, ZoneOffset.UTC));

    private CambiarIntervaloUseCase useCase;

    @BeforeEach
    void preparar() {
        useCase = new CambiarIntervaloUseCase(suscripciones, planes, proveedor, catalogo,
                sincronizador, bloqueo, propiedades);
        when(suscripciones.findById(ID)).thenReturn(Optional.of(
                suscripcion(EstadoSuscripcion.ACTIVA, false, IntervaloPlan.MENSUAL, "12.00")));
        when(planes.findPrecioActivo("PREMIUM", IntervaloPlan.ANUAL))
                .thenReturn(Optional.of(precio(IntervaloPlan.ANUAL, "120.00")));
        when(proveedor.precioPorLookupKey("acciones_ud_premium_anual"))
                .thenReturn(new PrecioProveedor("price_anual", 12000L, "usd", "acciones_ud_premium_anual"));
        when(proveedor.obtenerSuscripcion("sub_test")).thenReturn(remota("acciones_ud_premium_mensual", "si_1"));
        when(proveedor.cambiarPrecio(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(remota("acciones_ud_premium_anual", "si_2"));
        when(sincronizador.aplicar(any())).thenAnswer(invocacion -> entidad(invocacion.getArgument(0)));
    }

    @Test
    void cambiaElPrecioConElItemYElPrecioRemoto() {
        Suscripcion resultado = useCase.ejecutar(USUARIO, ID, IntervaloPlan.ANUAL);

        assertThat(resultado.getPrecioPlan().getIntervalo()).isEqualTo(IntervaloPlan.ANUAL);
        verify(bloqueo).bloquear(USUARIO);
        verify(proveedor).cambiarPrecio("sub_test", "si_1", "price_anual", "m10-cambiar-intervalo-8-3");
        verify(sincronizador).aplicar(any());
    }

    @Test
    void elMismoIntervaloNoLlamaAStripe() {
        Suscripcion resultado = useCase.ejecutar(USUARIO, ID, IntervaloPlan.MENSUAL);

        assertThat(resultado.getPrecioPlan().getIntervalo()).isEqualTo(IntervaloPlan.MENSUAL);
        verify(proveedor, never()).cambiarPrecio(anyString(), anyString(), anyString(), anyString());
        verify(sincronizador, never()).aplicar(any());
    }

    @Test
    void unaSuscripcionAjenaEs403() {
        assertThatThrownBy(() -> useCase.ejecutar(USUARIO + 1, ID, IntervaloPlan.ANUAL))
                .isInstanceOf(SuscripcionAjenaException.class);
        verify(proveedor, never()).cambiarPrecio(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void unaSuscripcionInexistenteEs404() {
        when(suscripciones.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.ejecutar(USUARIO, ID, IntervaloPlan.ANUAL))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void unaCancelacionProgramadaEs409() {
        when(suscripciones.findById(ID)).thenReturn(Optional.of(
                suscripcion(EstadoSuscripcion.ACTIVA, true, IntervaloPlan.MENSUAL, "12.00")));

        assertThatThrownBy(() -> useCase.ejecutar(USUARIO, ID, IntervaloPlan.ANUAL))
                .isInstanceOf(ConflictoEstadoException.class);
        verify(proveedor, never()).cambiarPrecio(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void unaSuscripcionEnMoraEs409() {
        when(suscripciones.findById(ID)).thenReturn(Optional.of(
                suscripcion(EstadoSuscripcion.EN_MORA, false, IntervaloPlan.MENSUAL, "12.00")));

        assertThatThrownBy(() -> useCase.ejecutar(USUARIO, ID, IntervaloPlan.ANUAL))
                .isInstanceOf(ConflictoEstadoException.class);
    }

    @Test
    void siStripeFallaNoSincronizaNada() {
        when(proveedor.cambiarPrecio(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new ProveedorPagosNoDisponibleException("Stripe no esta disponible"));

        assertThatThrownBy(() -> useCase.ejecutar(USUARIO, ID, IntervaloPlan.ANUAL))
                .isInstanceOf(ProveedorPagosNoDisponibleException.class);
        verify(sincronizador, never()).aplicar(any());
        verify(suscripciones, never()).save(any());
    }

    @Test
    void unPrecioRemotoInconsistenteEs503() {
        when(proveedor.precioPorLookupKey("acciones_ud_premium_anual"))
                .thenReturn(new PrecioProveedor("price_anual", 999L, "usd", "acciones_ud_premium_anual"));

        assertThatThrownBy(() -> useCase.ejecutar(USUARIO, ID, IntervaloPlan.ANUAL))
                .isInstanceOf(CatalogoInconsistenteException.class);
        verify(proveedor, never()).cambiarPrecio(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void unaSuscripcionSinItemsEnStripeEs503() {
        when(proveedor.obtenerSuscripcion("sub_test"))
                .thenReturn(remota("acciones_ud_premium_mensual", null));

        assertThatThrownBy(() -> useCase.ejecutar(USUARIO, ID, IntervaloPlan.ANUAL))
                .isInstanceOf(CatalogoInconsistenteException.class);
        verify(proveedor, never()).cambiarPrecio(anyString(), anyString(), anyString(), anyString());
    }

    private static Suscripcion suscripcion(EstadoSuscripcion estado, boolean cancelarAlFinal,
                                           IntervaloPlan intervalo, String monto) {
        Plan plan = Plan.crear("PREMIUM", "Acciones UD Premium", "Watchlist y alertas");
        PrecioPlan precio = PrecioPlan.crear(plan, intervalo, new BigDecimal(monto), "USD",
                intervalo == IntervaloPlan.MENSUAL
                        ? "acciones_ud_premium_mensual" : "acciones_ud_premium_anual");
        Suscripcion suscripcion = Suscripcion.crear(USUARIO, new DatosSuscripcion(
                "sub_test", precio, estado, AHORA, FIN, cancelarAlFinal, null, null), AHORA);
        asignar(suscripcion, "id", ID);
        asignar(suscripcion, "version", 3L);
        return suscripcion;
    }

    private static PrecioPlan precio(IntervaloPlan intervalo, String monto) {
        Plan plan = Plan.crear("PREMIUM", "Acciones UD Premium", "Watchlist y alertas");
        return PrecioPlan.crear(plan, intervalo, new BigDecimal(monto), "USD", "acciones_ud_premium_anual");
    }

    private static SuscripcionProveedor remota(String lookup, String itemId) {
        return new SuscripcionProveedor("sub_test", "active", false, null, null, null,
                USUARIO, "cus_test", lookup, AHORA, FIN, itemId);
    }

    private static Suscripcion entidad(SuscripcionProveedor remota) {
        IntervaloPlan intervalo = "acciones_ud_premium_anual".equals(remota.lookupKey())
                ? IntervaloPlan.ANUAL : IntervaloPlan.MENSUAL;
        Plan plan = Plan.crear("PREMIUM", "Acciones UD Premium", "Watchlist y alertas");
        PrecioPlan precio = PrecioPlan.crear(plan, intervalo,
                intervalo == IntervaloPlan.ANUAL ? new BigDecimal("120.00") : new BigDecimal("12.00"),
                "USD", remota.lookupKey());
        Suscripcion entidad = Suscripcion.crear(USUARIO, new DatosSuscripcion(
                remota.id(), precio, EstadoSuscripcion.ACTIVA, remota.periodoInicio(),
                remota.periodoFin(), false, null, null), AHORA);
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

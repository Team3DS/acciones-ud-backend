package co.edu.udistrital.acciones.m10suscripcionespagos.unit;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.ResultadoCheckout;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.CatalogoInconsistenteException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.ConflictoIdempotenciaException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.ProveedorPagosNoDisponibleException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.SolicitudInvalidaException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.SuscripcionVigenteException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.BloqueoUsuarioPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.ClienteStripeRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.PaymentProviderPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.PlanRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.SesionCheckoutRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.SuscripcionRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.proveedor.PrecioProveedor;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.proveedor.SesionProveedor;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.proveedor.SolicitudCheckout;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.service.CatalogoPreciosProveedor;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.usecase.IniciarSuscripcionUseCase;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.ClienteStripe;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.IntervaloPlan;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.Plan;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.PrecioPlan;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.SesionCheckout;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.Suscripcion;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.config.StripeProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IniciarSuscripcionUseCaseTest {

    private static final long USUARIO = 55001L;
    private static final String CLAVE = "clave-abc";
    private static final Instant AHORA = Instant.parse("2026-03-01T00:00:00Z");

    private final SuscripcionRepositoryPort suscripciones = mock(SuscripcionRepositoryPort.class);
    private final SesionCheckoutRepositoryPort sesiones = mock(SesionCheckoutRepositoryPort.class);
    private final ClienteStripeRepositoryPort clientes = mock(ClienteStripeRepositoryPort.class);
    private final PlanRepositoryPort planes = mock(PlanRepositoryPort.class);
    private final PaymentProviderPort proveedor = mock(PaymentProviderPort.class);
    private final BloqueoUsuarioPort bloqueo = mock(BloqueoUsuarioPort.class);

    private final StripeProperties propiedades = new StripeProperties(
            "sk_test_clave", "whsec_secreto", null, null, null, 2, null, null, null, null, null);
    private final Clock clock = Clock.fixed(AHORA, ZoneOffset.UTC);
    private final CatalogoPreciosProveedor catalogo = new CatalogoPreciosProveedor(proveedor, propiedades, clock);

    private IniciarSuscripcionUseCase useCase;
    private PrecioPlan precioMensual;

    @BeforeEach
    void preparar() {
        useCase = new IniciarSuscripcionUseCase(suscripciones, sesiones, clientes, planes,
                proveedor, catalogo, bloqueo, propiedades, clock);
        precioMensual = precio(IntervaloPlan.MENSUAL, "12.00", "acciones_ud_premium_mensual", 10L);
        when(suscripciones.findVigenteByUserId(USUARIO)).thenReturn(Optional.empty());
        when(sesiones.findByIdempotencyKey(USUARIO, CLAVE)).thenReturn(Optional.empty());
        when(sesiones.findAbiertaVigente(any(), any(), any())).thenReturn(Optional.empty());
        when(sesiones.findAbiertasByUserId(any(), any())).thenReturn(List.of());
        when(clientes.findById(USUARIO)).thenReturn(Optional.empty());
        when(sesiones.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    void creaLaSesionLaPrimeraVez() {
        when(planes.findPrecioActivo("PREMIUM", IntervaloPlan.MENSUAL)).thenReturn(Optional.of(precioMensual));
        when(proveedor.precioPorLookupKey("acciones_ud_premium_mensual"))
                .thenReturn(new PrecioProveedor("price_123", 1200L, "usd", "acciones_ud_premium_mensual"));
        when(proveedor.crearSesionCheckout(any())).thenReturn(new SesionProveedor(
                "cs_test_1", "https://stripe.test/cs_1", AHORA.plus(Duration.ofHours(1)), null, null));

        ResultadoCheckout resultado = useCase.ejecutar(USUARIO, CLAVE, IntervaloPlan.MENSUAL);

        assertThat(resultado.reutilizada()).isFalse();
        assertThat(resultado.sesion().getStripeSessionId()).isEqualTo("cs_test_1");
        verify(bloqueo).bloquear(USUARIO);
        verify(sesiones).save(any());
    }

    @Test
    void reutilizaLaSesionConLaMismaClaveYMismoIntervalo() {
        SesionCheckout existente = sesion(precioMensual, "cs_test_prev", CLAVE);
        when(sesiones.findByIdempotencyKey(USUARIO, CLAVE)).thenReturn(Optional.of(existente));

        ResultadoCheckout resultado = useCase.ejecutar(USUARIO, CLAVE, IntervaloPlan.MENSUAL);

        assertThat(resultado.reutilizada()).isTrue();
        assertThat(resultado.sesion().getStripeSessionId()).isEqualTo("cs_test_prev");
        verify(proveedor, never()).crearSesionCheckout(any());
    }

    @Test
    void laMismaClaveConOtroIntervaloEsConflicto() {
        SesionCheckout existente = sesion(precioMensual, "cs_test_prev", CLAVE);
        when(sesiones.findByIdempotencyKey(USUARIO, CLAVE)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> useCase.ejecutar(USUARIO, CLAVE, IntervaloPlan.ANUAL))
                .isInstanceOf(ConflictoIdempotenciaException.class);
    }

    @Test
    void reutilizaLaSesionAbiertaDelMismoIntervalo() {
        SesionCheckout abierta = sesion(precioMensual, "cs_test_abierta", "otra-clave");
        when(planes.findPrecioActivo("PREMIUM", IntervaloPlan.MENSUAL)).thenReturn(Optional.of(precioMensual));
        when(proveedor.precioPorLookupKey("acciones_ud_premium_mensual"))
                .thenReturn(new PrecioProveedor("price_123", 1200L, "usd", "acciones_ud_premium_mensual"));
        when(sesiones.findAbiertaVigente(USUARIO, 10L, AHORA)).thenReturn(Optional.of(abierta));

        ResultadoCheckout resultado = useCase.ejecutar(USUARIO, CLAVE, IntervaloPlan.MENSUAL);

        assertThat(resultado.reutilizada()).isTrue();
        assertThat(resultado.sesion().getStripeSessionId()).isEqualTo("cs_test_abierta");
        verify(proveedor, never()).crearSesionCheckout(any());
    }

    @Test
    void expiraLaSesionAbiertaDeOtroIntervaloAntesDeCrearLaNueva() {
        PrecioPlan precioAnual = precio(IntervaloPlan.ANUAL, "120.00", "acciones_ud_premium_anual", 20L);
        SesionCheckout otra = sesion(precioAnual, "cs_test_anual", "vieja");
        when(planes.findPrecioActivo("PREMIUM", IntervaloPlan.MENSUAL)).thenReturn(Optional.of(precioMensual));
        when(proveedor.precioPorLookupKey("acciones_ud_premium_mensual"))
                .thenReturn(new PrecioProveedor("price_123", 1200L, "usd", "acciones_ud_premium_mensual"));
        when(sesiones.findAbiertasByUserId(USUARIO, AHORA)).thenReturn(List.of(otra));
        when(proveedor.crearSesionCheckout(any())).thenReturn(new SesionProveedor(
                "cs_test_nueva", "https://stripe.test/nueva", AHORA.plus(Duration.ofHours(1)), null, null));

        ResultadoCheckout resultado = useCase.ejecutar(USUARIO, CLAVE, IntervaloPlan.MENSUAL);

        assertThat(resultado.reutilizada()).isFalse();
        assertThat(resultado.sesion().getStripeSessionId()).isEqualTo("cs_test_nueva");
        verify(proveedor).expirarSesion("cs_test_anual");
    }

    @Test
    void usaElClienteDeStripeExistente() {
        when(planes.findPrecioActivo("PREMIUM", IntervaloPlan.MENSUAL)).thenReturn(Optional.of(precioMensual));
        when(proveedor.precioPorLookupKey("acciones_ud_premium_mensual"))
                .thenReturn(new PrecioProveedor("price_123", 1200L, "usd", "acciones_ud_premium_mensual"));
        when(clientes.findById(USUARIO))
                .thenReturn(Optional.of(ClienteStripe.crear(USUARIO, "cus_abc", AHORA)));
        when(proveedor.crearSesionCheckout(any())).thenAnswer(invocacion -> {
            SolicitudCheckout solicitud = invocacion.getArgument(0);
            assertThat(solicitud.customerId()).isEqualTo("cus_abc");
            assertThat(solicitud.idempotencyKey()).isEqualTo("m10-checkout-" + USUARIO + "-" + CLAVE);
            return new SesionProveedor("cs_x", "https://stripe.test/x",
                    AHORA.plus(Duration.ofHours(1)), null, null);
        });

        ResultadoCheckout resultado = useCase.ejecutar(USUARIO, CLAVE, IntervaloPlan.MENSUAL);

        assertThat(resultado.sesion().getStripeSessionId()).isEqualTo("cs_x");
    }

    @Test
    void siElUsuarioTieneSuscripcionVigenteResponde409() {
        when(suscripciones.findVigenteByUserId(USUARIO)).thenReturn(Optional.of(mock(Suscripcion.class)));

        assertThatThrownBy(() -> useCase.ejecutar(USUARIO, CLAVE, IntervaloPlan.MENSUAL))
                .isInstanceOf(SuscripcionVigenteException.class);
        verify(proveedor, never()).crearSesionCheckout(any());
    }

    @Test
    void siElPrecioDeStripeNoCoincideConElCatalogoResponde503() {
        when(planes.findPrecioActivo("PREMIUM", IntervaloPlan.MENSUAL)).thenReturn(Optional.of(precioMensual));
        when(proveedor.precioPorLookupKey("acciones_ud_premium_mensual"))
                .thenReturn(new PrecioProveedor("price_123", 999L, "usd", "acciones_ud_premium_mensual"));

        assertThatThrownBy(() -> useCase.ejecutar(USUARIO, CLAVE, IntervaloPlan.MENSUAL))
                .isInstanceOf(CatalogoInconsistenteException.class);
        verify(proveedor, never()).crearSesionCheckout(any());
    }

    @Test
    void siNoHayPrecioEnElCatalogoLocalResponde503() {
        when(planes.findPrecioActivo("PREMIUM", IntervaloPlan.MENSUAL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.ejecutar(USUARIO, CLAVE, IntervaloPlan.MENSUAL))
                .isInstanceOf(CatalogoInconsistenteException.class);
    }

    @Test
    void siStripeNoEstaConfiguradoResponde503() {
        StripeProperties sinClave = new StripeProperties(
                "", "", null, null, null, 2, null, null, null, null, null);
        IniciarSuscripcionUseCase sinStripe = new IniciarSuscripcionUseCase(suscripciones, sesiones,
                clientes, planes, proveedor, catalogo, bloqueo, sinClave, clock);

        assertThatThrownBy(() -> sinStripe.ejecutar(USUARIO, CLAVE, IntervaloPlan.MENSUAL))
                .isInstanceOf(ProveedorPagosNoDisponibleException.class);
        verify(bloqueo, never()).bloquear(any());
    }

    @Test
    void rechazaUnaClaveDeIdempotenciaInvalida() {
        assertThatThrownBy(() -> useCase.ejecutar(USUARIO, "clave con espacios", IntervaloPlan.MENSUAL))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void rechazaUnUsuarioNoPositivo() {
        assertThatThrownBy(() -> useCase.ejecutar(0L, CLAVE, IntervaloPlan.MENSUAL))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    private static PrecioPlan precio(IntervaloPlan intervalo, String monto, String lookupKey, long id) {
        Plan plan = Plan.crear("PREMIUM", "Acciones UD Premium", "Watchlist y alertas");
        PrecioPlan precio = PrecioPlan.crear(plan, intervalo, new BigDecimal(monto), "USD", lookupKey);
        asignarId(precio, id);
        return precio;
    }

    private SesionCheckout sesion(PrecioPlan precio, String stripeSessionId, String idempotencyKey) {
        return SesionCheckout.crear(USUARIO, precio, stripeSessionId, "https://stripe.test/" + stripeSessionId,
                idempotencyKey, AHORA, AHORA.plus(Duration.ofHours(1)));
    }

    private static void asignarId(PrecioPlan precio, long id) {
        try {
            Field campo = PrecioPlan.class.getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(precio, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}

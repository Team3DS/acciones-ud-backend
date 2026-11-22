package co.edu.udistrital.acciones.m10suscripcionespagos.unit;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.RoutingKeys;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.payload.SuscripcionActivadaPayload;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.payload.SuscripcionCanceladaPayload;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.payload.SuscripcionVencidaPayload;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.SuscripcionAjenaException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.ClienteStripeRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.OutboxPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.PaymentProviderPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.PlanRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.SesionCheckoutRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.SuscripcionRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.proveedor.SuscripcionProveedor;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.service.SincronizadorSuscripciones;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.DatosSuscripcion;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.EstadoSuscripcion;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.IntervaloPlan;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.Plan;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.PrecioPlan;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.Suscripcion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SincronizadorSuscripcionesTest {

    private static final long USUARIO = 9001L;
    private static final Instant AHORA = Instant.parse("2026-05-01T00:00:00Z");
    private static final Instant INICIO = Instant.parse("2026-05-01T00:00:00Z");
    private static final Instant FIN = Instant.parse("2026-06-01T00:00:00Z");

    private final PaymentProviderPort proveedor = mock(PaymentProviderPort.class);
    private final SuscripcionRepositoryPort suscripciones = mock(SuscripcionRepositoryPort.class);
    private final PlanRepositoryPort planes = mock(PlanRepositoryPort.class);
    private final SesionCheckoutRepositoryPort sesiones = mock(SesionCheckoutRepositoryPort.class);
    private final ClienteStripeRepositoryPort clientes = mock(ClienteStripeRepositoryPort.class);
    private final OutboxPort outbox = mock(OutboxPort.class);
    private final Clock clock = Clock.fixed(AHORA, ZoneOffset.UTC);

    private SincronizadorSuscripciones sincronizador;
    private PrecioPlan precio;

    @BeforeEach
    void preparar() {
        sincronizador = new SincronizadorSuscripciones(proveedor, suscripciones, planes, sesiones,
                clientes, outbox, clock);
        precio = precio();
        when(planes.findPrecioPorLookupKey("acciones_ud_premium_mensual")).thenReturn(Optional.of(precio));
        when(suscripciones.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void laPrimeraSincronizacionActivaYPublicaActivada() {
        when(suscripciones.findByStripeSubscriptionIdForUpdate("sub_1")).thenReturn(Optional.empty());
        when(sesiones.existePorUserId(USUARIO)).thenReturn(true);
        when(clientes.findById(USUARIO)).thenReturn(Optional.empty());

        sincronizador.aplicar(remota("sub_1", "active", null, false, null));

        verify(outbox).registrar(eq(RoutingKeys.TIPO_AGREGADO_SUSCRIPCION), any(),
                eq(RoutingKeys.SUSCRIPCION_ACTIVADA), any(SuscripcionActivadaPayload.class));
    }

    @Test
    void siElPrecioNoEstaEnElCatalogoEsAjena() {
        when(planes.findPrecioPorLookupKey("desconocido")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sincronizador.aplicar(remotaConLookup("sub_x", "active", "desconocido")))
                .isInstanceOf(SuscripcionAjenaException.class);
        verify(outbox, never()).registrar(any(), any(), any(), any());
    }

    @Test
    void siElUsuarioNoTieneSesionEsAjena() {
        when(suscripciones.findByStripeSubscriptionIdForUpdate("sub_2")).thenReturn(Optional.empty());
        when(sesiones.existePorUserId(USUARIO)).thenReturn(false);

        assertThatThrownBy(() -> sincronizador.aplicar(remota("sub_2", "active", null, false, null)))
                .isInstanceOf(SuscripcionAjenaException.class);
    }

    @Test
    void laRenovacionSobreActivaNoPublicaEvento() {
        Suscripcion activa = suscripcionExistente(EstadoSuscripcion.ACTIVA);
        when(suscripciones.findByStripeSubscriptionIdForUpdate("sub_1")).thenReturn(Optional.of(activa));

        sincronizador.aplicar(remota("sub_1", "active", null, false, null));

        verify(outbox, never()).registrar(any(), any(), any(), any());
    }

    @Test
    void elPasoDeEnMoraAActivaPublicaActivada() {
        Suscripcion enMora = suscripcionExistente(EstadoSuscripcion.EN_MORA);
        when(suscripciones.findByStripeSubscriptionIdForUpdate("sub_1")).thenReturn(Optional.of(enMora));

        sincronizador.aplicar(remota("sub_1", "active", null, false, null));

        verify(outbox).registrar(eq(RoutingKeys.TIPO_AGREGADO_SUSCRIPCION), any(),
                eq(RoutingKeys.SUSCRIPCION_ACTIVADA), any(SuscripcionActivadaPayload.class));
    }

    @Test
    void laCancelacionPorElUsuarioPublicaCancelada() {
        Suscripcion activa = suscripcionExistente(EstadoSuscripcion.ACTIVA);
        when(suscripciones.findByStripeSubscriptionIdForUpdate("sub_1")).thenReturn(Optional.of(activa));

        sincronizador.aplicar(remota("sub_1", "canceled", "cancellation_requested", true, FIN));

        verify(outbox).registrar(eq(RoutingKeys.TIPO_AGREGADO_SUSCRIPCION), any(),
                eq(RoutingKeys.SUSCRIPCION_CANCELADA), any(SuscripcionCanceladaPayload.class));
    }

    @Test
    void elImpagoPublicaVencida() {
        Suscripcion enMora = suscripcionExistente(EstadoSuscripcion.EN_MORA);
        when(suscripciones.findByStripeSubscriptionIdForUpdate("sub_1")).thenReturn(Optional.of(enMora));

        sincronizador.aplicar(remota("sub_1", "canceled", "payment_failed", false, FIN));

        verify(outbox).registrar(eq(RoutingKeys.TIPO_AGREGADO_SUSCRIPCION), any(),
                eq(RoutingKeys.SUSCRIPCION_VENCIDA), any(SuscripcionVencidaPayload.class));
    }

    @Test
    void elPasoAEnMoraNoPublicaEventoDeEstado() {
        Suscripcion activa = suscripcionExistente(EstadoSuscripcion.ACTIVA);
        when(suscripciones.findByStripeSubscriptionIdForUpdate("sub_1")).thenReturn(Optional.of(activa));

        sincronizador.aplicar(remota("sub_1", "past_due", null, false, null));

        verify(outbox, never()).registrar(any(), any(), any(), any());
    }

    private Suscripcion suscripcionExistente(EstadoSuscripcion estado) {
        Suscripcion suscripcion = Suscripcion.crear(USUARIO, new DatosSuscripcion(
                "sub_1", precio, EstadoSuscripcion.ACTIVA, INICIO, FIN, false, null, null), INICIO);
        if (estado != EstadoSuscripcion.ACTIVA) {
            suscripcion.sincronizar(new DatosSuscripcion(
                    "sub_1", precio, estado, INICIO, FIN, false, null, null), INICIO);
        }
        asignarId(suscripcion);
        return suscripcion;
    }

    private SuscripcionProveedor remota(String id, String estado, String motivo,
                                        boolean cancelarAlFinal, Instant finalizadaEn) {
        return new SuscripcionProveedor(id, estado, cancelarAlFinal, null, finalizadaEn, motivo,
                USUARIO, "cus_1", "acciones_ud_premium_mensual", INICIO, FIN, "si_1");
    }

    private SuscripcionProveedor remotaConLookup(String id, String estado, String lookup) {
        return new SuscripcionProveedor(id, estado, false, null, null, null,
                USUARIO, "cus_1", lookup, INICIO, FIN, "si_1");
    }

    private static PrecioPlan precio() {
        Plan plan = Plan.crear("PREMIUM", "Acciones UD Premium", "Watchlist y alertas");
        return PrecioPlan.crear(plan, IntervaloPlan.MENSUAL, new BigDecimal("12.00"),
                "USD", "acciones_ud_premium_mensual");
    }

    private static void asignarId(Suscripcion suscripcion) {
        try {
            Field campo = Suscripcion.class.getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(suscripcion, 500L);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}

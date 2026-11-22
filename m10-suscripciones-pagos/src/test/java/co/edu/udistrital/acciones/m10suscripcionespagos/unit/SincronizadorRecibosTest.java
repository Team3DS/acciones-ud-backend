package co.edu.udistrital.acciones.m10suscripcionespagos.unit;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.RoutingKeys;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.payload.PagoFallidoPayload;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.OutboxPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.PaymentProviderPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.ReciboRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.proveedor.ReciboProveedor;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.service.SincronizadorRecibos;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.service.SincronizadorSuscripciones;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.DatosSuscripcion;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.EstadoRecibo;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.EstadoSuscripcion;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.IntervaloPlan;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.Plan;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.PrecioPlan;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.Recibo;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SincronizadorRecibosTest {

    private static final long USUARIO = 9100L;
    private static final Instant AHORA = Instant.parse("2026-05-10T00:00:00Z");
    private static final Instant INICIO = Instant.parse("2026-05-01T00:00:00Z");
    private static final Instant FIN = Instant.parse("2026-06-01T00:00:00Z");

    private final PaymentProviderPort proveedor = mock(PaymentProviderPort.class);
    private final ReciboRepositoryPort recibos = mock(ReciboRepositoryPort.class);
    private final SincronizadorSuscripciones sincronizadorSuscripciones = mock(SincronizadorSuscripciones.class);
    private final OutboxPort outbox = mock(OutboxPort.class);
    private final Clock clock = Clock.fixed(AHORA, ZoneOffset.UTC);

    private SincronizadorRecibos sincronizador;
    private Suscripcion suscripcion;

    @BeforeEach
    void preparar() {
        sincronizador = new SincronizadorRecibos(proveedor, recibos,
                sincronizadorSuscripciones, outbox, clock);
        suscripcion = suscripcion();
        when(recibos.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void elReciboSinSuscripcionSeDescarta() {
        when(proveedor.obtenerRecibo("in_1")).thenReturn(recibo("in_1", "paid", 0, null));

        sincronizador.sincronizar("in_1");

        verify(recibos, never()).save(any());
        verify(outbox, never()).registrar(any(), any(), any(), any());
    }

    @Test
    void unReciboPagadoNuevoSeGuardaSinEvento() {
        when(proveedor.obtenerRecibo("in_2")).thenReturn(recibo("in_2", "paid", 0, "sub_1"));
        when(sincronizadorSuscripciones.sincronizar("sub_1")).thenReturn(suscripcion);
        when(recibos.findByStripeInvoiceId("in_2")).thenReturn(Optional.empty());

        sincronizador.sincronizar("in_2");

        verify(recibos).save(any());
        verify(outbox, never()).registrar(any(), any(), any(), any());
    }

    @Test
    void unReciboFallidoNuevoPublicaPagoFallido() {
        when(proveedor.obtenerRecibo("in_3")).thenReturn(recibo("in_3", "open", 1, "sub_1"));
        when(sincronizadorSuscripciones.sincronizar("sub_1")).thenReturn(suscripcion);
        Recibo guardado = reciboLocal(EstadoRecibo.FALLIDO, 1);
        when(recibos.findByStripeInvoiceId("in_3"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(guardado));

        sincronizador.sincronizar("in_3");

        verify(outbox).registrar(eq(RoutingKeys.TIPO_AGREGADO_RECIBO), any(),
                eq(RoutingKeys.PAGO_FALLIDO), any(PagoFallidoPayload.class));
    }

    @Test
    void siempreSincronizaLaSuscripcionAntesDeGuardarElRecibo() {
        when(proveedor.obtenerRecibo("in_4")).thenReturn(recibo("in_4", "paid", 0, "sub_9"));
        when(sincronizadorSuscripciones.sincronizar("sub_9")).thenReturn(suscripcion);
        when(recibos.findByStripeInvoiceId("in_4")).thenReturn(Optional.empty());

        sincronizador.sincronizar("in_4");

        verify(sincronizadorSuscripciones).sincronizar("sub_9");
        verify(recibos).save(any());
    }

    private Recibo reciboLocal(EstadoRecibo estado, int intento) {
        Recibo recibo = Recibo.crear(500L, "in_x",
                co.edu.udistrital.acciones.m10suscripcionespagos.domain.Dinero.desdeCentavos(1200),
                estado, intento, AHORA, AHORA);
        try {
            Field campo = Recibo.class.getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(recibo, 42L);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
        return recibo;
    }

    private static ReciboProveedor recibo(String id, String estado, long intento, String sub) {
        return new ReciboProveedor(id, "A-1", 1200L, "usd", estado, INICIO, FIN, AHORA,
                "paid".equals(estado) ? AHORA : null, intento,
                "https://stripe.test/r/" + id, "https://stripe.test/r/" + id + ".pdf", sub);
    }

    private Suscripcion suscripcion() {
        Plan plan = Plan.crear("PREMIUM", "Acciones UD Premium", "Watchlist y alertas");
        PrecioPlan precio = PrecioPlan.crear(plan, IntervaloPlan.MENSUAL, new BigDecimal("12.00"),
                "USD", "acciones_ud_premium_mensual");
        Suscripcion suscripcion = Suscripcion.crear(USUARIO, new DatosSuscripcion(
                "sub_1", precio, EstadoSuscripcion.ACTIVA, INICIO, FIN, false, null, null), INICIO);
        try {
            Field campo = Suscripcion.class.getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(suscripcion, 500L);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
        return suscripcion;
    }
}
